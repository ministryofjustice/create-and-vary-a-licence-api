package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient

@Service
class PrisonerUpdatedHandler(
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  @param:Value("\${update.offender.details.handler.enabled}") private val updateOffenderDetailsHandleEnabled: Boolean,
) {
  companion object {
    private val log = LoggerFactory.getLogger(PrisonerUpdatedHandler::class.java)
  }

  fun handleEvent(message: String) {
    if (updateOffenderDetailsHandleEnabled) {
      val event = objectMapper.readValue(message, HMPPSPrisonerUpdatedEvent::class.java)
      if (event.additionalInformation.categoriesChanged.contains(DiffCategory.PERSONAL_DETAILS)) {
        updatePrisonerDetails(event.additionalInformation.nomsNumber)
      }
    }
  }

  fun updatePrisonerDetails(nomsId: String) {
    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId)).first()

    log.info("processing offender updated event for nomsId: $nomsId")
    offenderService.updateOffenderDetails(
      nomsId,
      UpdateOffenderDetailsRequest(
        forename = prisoner.firstName.convertToTitleCase(),
        middleNames = prisoner.middleNames?.convertToTitleCase(),
        surname = prisoner.lastName.convertToTitleCase(),
        dateOfBirth = prisoner.dateOfBirth,
      ),
    )
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
