package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.createMapper

class PrisonerUpdatedHandlerTest {
  private val mapper: ObjectMapper = createMapper()
  private val offenderService = mock<OffenderService>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()

  @Test
  fun `should process prisoner updated event`() {
    val handler = PrisonerUpdatedHandler(
      mapper,
      offenderService,
      prisonerSearchApiClient,
      updateOffenderDetailsHandleEnabled = true,
    )
    val nomsId = "A1234AA"

    val prisoner = prisonerSearchResult().copy(firstName = "ABCDEF", middleNames = null)
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
        forename = prisoner.firstName.convertToTitleCase(),
        middleNames = "",
        surname = prisoner.lastName.convertToTitleCase(),
        dateOfBirth = prisoner.dateOfBirth,
      ),
    )
  }

  @Test
  fun `should not update prisoner details when categories do not include person details`() {
    val handler = PrisonerUpdatedHandler(
      mapper,
      offenderService,
      prisonerSearchApiClient,
      updateOffenderDetailsHandleEnabled = true,
    )
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

  @Test
  fun `should not process prisoner updated event if the feature flag is turned off`() {
    val handler = PrisonerUpdatedHandler(
      mapper,
      offenderService,
      prisonerSearchApiClient,
      updateOffenderDetailsHandleEnabled = false,
    )
    val nomsId = "A1234AA"

    handler.handleEvent(
      aPrisonerUpdatedEventMessage(
        nomsId,
        listOf(DiffCategory.ALERTS, DiffCategory.PERSONAL_DETAILS),
      ),
    )

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(offenderService)
  }

  private fun aPrisonerUpdatedEventMessage(nomsId: String, categories: List<DiffCategory>) = mapper.writeValueAsString(
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
