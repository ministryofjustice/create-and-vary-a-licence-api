package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient

@Service
class PrisonerUpdatedHandler(
  private val objectMapper: ObjectMapper,
  private val offenderService: OffenderService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  fun handleEvent(message: String) {
    val event = objectMapper.readValue(message, HMPPSPrisonerUpdatedEvent::class.java)
    if (event.additionalInformation.categoriesChanged.contains(DiffCategory.PERSONAL_DETAILS)) {
      println(event)
    }
    updatePrisonerDetails(event.additionalInformation.nomsNumber)
  }

  fun updatePrisonerDetails(nomsId: String) {
    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId)).first()

    offenderService.updateOffenderDetails(
      nomsId,
      UpdateOffenderDetailsRequest(
        forename = prisoner.firstName,
        middleNames = prisoner.middleNames,
        surname = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
      ),
    )
  }
}

data class HMPPSPrisonerUpdatedEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformationPrisonerUpdated,
  val version: String,
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
