package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient

class PrisonerUpdatedHandlerTest {
  private val objectMapper = jacksonObjectMapper()
  private val offenderService = mock<OffenderService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()

  private val handler = PrisonerUpdatedHandler(objectMapper, offenderService, prisonerSearchApiClient)

  @Test
  fun `should process prisoner updated event`() {
    val nomsId = "A1234AA"

    val prisoner = prisonerSearchResult()
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsId))).thenReturn(listOf(prisoner))

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        nomsId,
        listOf(DiffCategory.ALERTS, DiffCategory.PERSONAL_DETAILS),
      ),
    )

    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf(nomsId))

    verify(offenderService).updateOffenderDetails(
      nomsId,
      UpdateOffenderDetailsRequest(
        forename = prisoner.firstName,
        middleNames = prisoner.middleNames,
        surname = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
      ),
    )
  }

  @Test
  fun `should not update prisoner details when categories do not include person details`() {
    val nomsId = "A1294AC"

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        nomsId,
        listOf(DiffCategory.PHYSICAL_DETAILS, DiffCategory.INCENTIVE_LEVEL),
      ),
    )

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(offenderService)
  }

  private fun aPrisonerUpdatedEventMessage(nomsId: String, categories: List<DiffCategory>) = jacksonObjectMapper().writeValueAsString(
    HMPPSPrisonerUpdatedEvent(
      eventType = COM_ALLOCATED_EVENT_TYPE,
      additionalInformation = AdditionalInformationPrisonerUpdated(
        nomsNumber = nomsId,
        categoriesChanged = categories,
      ),
      version = 0,
      occurredAt = "2023-12-05T00:00:00Z",
      description = "prisoner updated",
    ),
  )
}
