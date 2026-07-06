package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.slf4j.LoggerFactory
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
) {
  companion object {
    private val log = LoggerFactory.getLogger(PrisonerUpdatedHandler::class.java)
  }

  @Transactional
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
    log.info("Processing prisoner updated event received for nomis id: $nomsId")

    val nomisRecord = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId)).first()

    if (!nomisRecord.isRestrictedPatient()) {
      log.info("Nomis record is not a restricted patient, skipping prisoner updated event")
      return
    }
    val licences = getLicences(nomsId, LicenceStatus.PRE_RELEASE_STATUSES.toList())

    if (licences.isEmpty()) {
      log.info("No in-flight licences found for nomsId: $nomsId, skipping prisoner updated event")
      return
    }

    val prisonInformation = prisonApiClient.getPrisonInformation(nomisRecord.supportingPrisonId!!)

    updateLicences(licences, prisonInformation)

    log.info("Processed prisoner updated event for nomis id: $nomsId")
  }

  private fun getLicences(nomisId: String, licenceStatuses: List<LicenceStatus>): List<Licence> = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomisId, licenceStatuses)

  fun updateLicences(licences: List<Licence>, prisonInformation: Prison) {
    licences.map { licence ->
      val previousPrisonCode = licence.prisonCode
      if (previousPrisonCode == prisonInformation.prisonId) {
        log.info("Prison code for licence id ${licence.id} is already ${prisonInformation.prisonId}, skipping prisoner updated event")
        return@map
      }
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
