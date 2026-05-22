package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateOffenderDetailsEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.Prison
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class PrisonerUpdatedHandler(
  private val mapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val licenceRepository: LicenceRepository,
  private val prisonApiClient: PrisonApiClient,
  private val staffRepository: StaffRepository,
  private val auditEventRepository: AuditEventRepository,
  @param:Value("\${feature.toggle.restrictedPatients.enabled:false}") private val restrictedPatientsEnabled: Boolean = false,

) {
  companion object {
    private val log = LoggerFactory.getLogger(PrisonerUpdatedHandler::class.java)
  }

  fun handleEvent(message: String) {
    val event = mapper.readValue(message, HMPPSPrisonerUpdatedEvent::class.java)
    val categoriesChanged = event.additionalInformation.categoriesChanged
    val nomsNumber = event.additionalInformation.nomsNumber
    if (DiffCategory.PERSONAL_DETAILS in categoriesChanged) {
      updatePrisonerDetails(nomsNumber)
    }
    if (DiffCategory.RESTRICTED_PATIENT in categoriesChanged) {
      updateSupportingPrisonId(nomsNumber)
    }
  }

  fun updatePrisonerDetails(nomsId: String) {
    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId)).first()

    log.info("processing offender updated event for nomsId: $nomsId")
    offenderService.updateOffenderDetails(
      nomsId,
      UpdateOffenderDetailsEvent(
        forename = prisoner.firstName.convertToTitleCase(),
        middleNames = if (prisoner.middleNames == null) "" else prisoner.middleNames.convertToTitleCase(),
        surname = prisoner.lastName.convertToTitleCase(),
        dateOfBirth = prisoner.dateOfBirth,
      ),
    )
  }

  fun updateSupportingPrisonId(nomsId: String) {
    if (restrictedPatientsEnabled) {
      val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId)).first()
      if (!nomisRecord.isRestrictedPatient()) {
        log.info("Nomis record is not a restricted patient, skipping update of supporting prison")
        return
      }
      val licences = getLicences(nomsId, LicenceStatus.PRE_RELEASE_STATUSES.toList())

      if (licences.isEmpty()) {
        log.info("No in-flight licences found for nomsId: $nomsId, skipping update of supporting prison")
        return
      }

      val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.supportingPrisonId!!)

      log.info("Updating supporting prison information for ${licences.size} licences for nomisId: $nomsId with prison code: ${prisonInformation.prisonId}, description: ${prisonInformation.description}, telephone: ${prisonInformation.getPrisonContactNumber()}")

      updateLicences(licences, prisonInformation)

      log.info("Processed supporting prison changed event received for nomis id: $nomsId and updated ${licences.size} licences")
    } else {
      log.info("Restricted patients feature is disabled, skipping handling of supporting prison changed event")
    }
  }

  private fun getLicences(nomisId: String, licenceStatuses: List<LicenceStatus>): List<Licence> = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomisId, licenceStatuses)

  private fun updateLicences(licences: List<Licence>, prisonInformation: Prison) {
    licences.forEach { licence ->
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

data class HMPPSPrisonerUpdatedEvent(
  val eventType: String? = PRISONER_UPDATED_EVENT_TYPE,
  val additionalInformation: AdditionalInformationPrisonerUpdated,
  val version: Int,
  val occurredAt: String,
  val description: String,
)

data class AdditionalInformationPrisonerUpdated(
  val nomsNumber: String,
  val categoriesChanged: List<DiffCategory>,
)

enum class DiffCategory {
  IDENTIFIERS,
  PERSONAL_DETAILS,
  ALERTS,
  STATUS,
  LOCATION,
  SENTENCE,
  RESTRICTED_PATIENT,
  INCENTIVE_LEVEL,
  PHYSICAL_DETAILS,
  CONTACT_DETAILS,
}
