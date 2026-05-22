package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.core.JacksonException
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class SupportingPrisonUpdatedHandler(
  private val mapper: ObjectMapper,
  private val prisonService: PrisonService,
  private val licenceRepository: LicenceRepository,
  private val prisonApiClient: PrisonApiClient,
  private val staffRepository: StaffRepository,
  private val auditEventRepository: AuditEventRepository,
  @param:Value("\${feature.toggle.restrictedPatients.enabled:false}") private val restrictedPatientsEnabled: Boolean = false,
) {
  companion object {
    private val log = LoggerFactory.getLogger(PrisonerUpdatedHandler::class.java)
  }

  @Transactional
  fun handleEvent(message: String) {
    if (restrictedPatientsEnabled) {
      val event = readEvent(message)
      val nomisId = event.personReference.noms()

      if (nomisId.isNullOrBlank()) {
        log.error("No nomis id found in supporting prison changed event: $message")
        return
      }

      log.info("Processing supporting prison changed event received for nomis id: $nomisId")

      val nomisRecord = prisonService.searchPrisonersByNomisIds(listOf(nomisId)).first()
      if (!nomisRecord.isRestrictedPatient()) {
        log.info("Nomis record is not a restricted patient, skipping update of supporting prison")
        return
      }

      val licences = getLicences(nomisId, LicenceStatus.PRE_RELEASE_STATUSES.toList())

      if (licences.isEmpty()) {
        log.info("No in-flight licences found for nomisId: $nomisId, skipping update of supporting prison")
        return
      }

      val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.supportingPrisonId!!)

      log.info("Updating supporting prison information for ${licences.size} licences for nomisId: $nomisId with prison code: ${prisonInformation.prisonId}, description: ${prisonInformation.description}, telephone: ${prisonInformation.getPrisonContactNumber()}")

      updateLicences(licences, prisonInformation)

      log.info("Processed supporting prison changed event received for nomis id: $nomisId and updated ${licences.size} licences")
    } else {
      log.info("Restricted patients feature is disabled, skipping handling of supporting prison changed event")
    }
  }

  private fun readEvent(message: String): HMPPSDomainEvent = try {
    mapper.readValue(message, HMPPSDomainEvent::class.java)
  } catch (e: JacksonException) {
    log.error("Failed to parse supporting prison changed event message", e)
    throw e
  }

  private fun getLicences(nomisId: String, licenceStatuses: List<LicenceStatus>): List<Licence> = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomisId, licenceStatuses)

  private fun updateLicences(licences: List<Licence>, prisonInformation: Prison) {
    licences.map { licence ->
      val previousPrisonCode = licence.prisonCode
      val user =
        staffRepository.findByUsernameIgnoreCase(
          SecurityContextHolder.getContext().authentication?.name ?: SYSTEM_USER,
        )

      licence.updatePrisonInfo(
        prisonCode = prisonInformation.prisonId,
        prisonDescription = prisonInformation.description,
        prisonTelephone = prisonInformation.getPrisonContactNumber(),
        staffMember = user,
      )

      licenceRepository.saveAndFlush(licence)

      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = licence.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "Supporting prison information changed for ${licence.forename} ${licence.surname}",
          detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode} version ${licence.version}",
          changes = mapOf(
            "field" to "prisonCode",
            "previousValue" to (previousPrisonCode ?: ""),
            "newValue" to (licence.prisonCode ?: ""),
          ),
        ),
      )
    }
  }
}
