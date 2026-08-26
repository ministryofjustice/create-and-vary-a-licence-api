package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class PrisonerReceivedHandler(
  private val mapper: ObjectMapper,
  private val prisonApiClient: PrisonApiClient,
  private val staffRepository: StaffRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceRepository: LicenceRepository,
  @param:Value("\${feature.toggle.remand.enabled:false}")
  private val remandEnabled: Boolean,
) : EventHandler {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  override fun handleEvent(message: String) {
    log.info("Received prisoner received event")
    val event = mapper.readValue(message, HMPPSDomainEvent::class.java)

    val additionalInformation = mapper.convertValue(event.additionalInformation, AdditionalInformationPrisonerReceived::class.java)
    val reason = additionalInformation.reason
    val prisonId = additionalInformation.prisonId
    val nomsId = additionalInformation.nomsNumber
    val validPrisonerReceivedStatuses = if (remandEnabled) LicenceStatus.PRISONER_RECEIVED_VALID_STATUSES else LicenceStatus.PRISONER_RECEIVED_VALID_STATUSES - LicenceStatus.ACTIVE
    val validPrisonerReceivedReasons = if (remandEnabled) listOf("ADMISSION", "TRANSFERRED") else listOf("TRANSFERRED")

    if (reason in validPrisonerReceivedReasons) {
      val licences = getLicences(nomsId, validPrisonerReceivedStatuses.toList())

      if (licences.isEmpty()) {
        log.info("No licences found for nomsId: $nomsId, skipping prisoner received event")
        return
      }

      val prisonInformation = prisonApiClient.getPrisonInformation(prisonId)

      updateLicences(licences, prisonInformation)
    } else {
      log.info("Received event for prisoner with reason: $reason, skipping prisoner received event")
    }
  }

  private fun getLicences(nomsId: String, licenceStatuses: List<LicenceStatus>): List<Licence> = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, licenceStatuses)

  private fun updateLicences(licences: List<Licence>, prisonInformation: Prison) {
    licences.map {
      val previousPrisonCode = it.prisonCode
      if (previousPrisonCode == prisonInformation.prisonId) {
        log.info("Prison code for licence id ${it.id} is already ${prisonInformation.prisonId}, skipping prisoner received event")
        return@map
      } else {
        log.info("Updating prison code for licence id ${it.id}")
      }

      val user =
        staffRepository.findByUsernameIgnoreCase(
          SecurityContextHolder.getContext().authentication?.name ?: SYSTEM_USER,
        )

      it.updatePrisonInfo(
        prisonCode = prisonInformation.prisonId,
        prisonDescription = prisonInformation.description,
        prisonTelephone = prisonInformation.getPrisonContactNumber(),
        staffMember = user,
      )

      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = it.id,
          username = SYSTEM_USER,
          fullName = SYSTEM_USER,
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "Prison information changed for ${it.forename} ${it.surname} on prisoner receive event",
          detail = "ID ${it.id} type ${it.typeCode} status ${it.statusCode} version ${it.version}",
          changes = mapOf(
            "field" to "prisonCode",
            "previousValue" to (previousPrisonCode ?: ""),
            "newValue" to (it.prisonCode ?: ""),
          ),
        ),
      )
      log.info("Updated prison code for licence id ${it.id} from $previousPrisonCode to ${it.prisonCode}")
    }
  }

  private data class AdditionalInformationPrisonerReceived(val reason: String, val prisonId: String, val nomsNumber: String)
}
