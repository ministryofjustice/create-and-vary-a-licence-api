package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService.UpdateType.SUPPORTING_PRISON_UPDATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateOffenderDetailsEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class PrisonerUpdatedHandler(
  private val mapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val licenceRepository: LicenceRepository,
  private val prisonInformationService: PrisonInformationService,
) : EventHandler {
  companion object {
    private val log = LoggerFactory.getLogger(PrisonerUpdatedHandler::class.java)
  }

  @Transactional
  override fun handleEvent(message: String) {
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
    val licences = getLicences(nomsId)

    if (licences.isEmpty()) {
      log.info("No in-flight licences found for nomsId: $nomsId, skipping prisoner updated event")
      return
    }

    prisonInformationService.updatePrisonInformation(
      SUPPORTING_PRISON_UPDATE,
      licences,
      nomisRecord.supportingPrisonId!!,
    )

    log.info("Processed prisoner updated event for nomis id: $nomsId")
  }

  private fun getLicences(nomisId: String): List<Licence> = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomisId, LicenceStatus.PRE_RELEASE_STATUSES.toList())
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
