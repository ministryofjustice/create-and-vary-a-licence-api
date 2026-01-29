package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aPrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderSentencesAndOffences
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import java.time.LocalDate

class PrisonServiceTest {
  private val prisonApiClient = mock<PrisonApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()

  private val service = PrisonService(prisonApiClient, prisonerSearchApiClient)

  @Test
  fun `should get prisoner details`() {
    val nomisId = "A1234AA"
    val prisonerDetails = aPrisonApiPrisoner()

    whenever(prisonApiClient.getPrisonerDetail(nomisId)).thenReturn(prisonerDetails)

    val actualPrisonerDetails = service.getPrisonerDetail(nomisId)

    assertThat(actualPrisonerDetails).isEqualTo(prisonerDetails)
    verify(prisonApiClient).getPrisonerDetail(nomisId)
  }

  @Test
  fun `should search prisoners by booking ids`() {
    val bookingIds = listOf(98234L, 3242L)
    val apiSearchResult = listOf(prisonerSearchResult())

    whenever(prisonerSearchApiClient.searchPrisonersByBookingIds(bookingIds)).thenReturn(apiSearchResult)

    val actualResult = service.searchPrisonersByBookingIds(bookingIds)

    assertThat(actualResult).isEqualTo(apiSearchResult)
    verify(prisonerSearchApiClient).searchPrisonersByBookingIds(
      bookingIds,
    )
  }

  @Test
  fun `should get a prisoners latest sentence start date`() {
    val bookingId = 87L
    val sentencesAndOffences = offenderSentencesAndOffences(bookingId)

    whenever(prisonApiClient.getPrisonerSentenceAndOffences(bookingId)).thenReturn(sentencesAndOffences)

    val actualDate = service.getPrisonerLatestSentenceStartDate(bookingId)

    assertThat(actualDate).isEqualTo(LocalDate.of(2025, 8, 25))
    verify(prisonApiClient).getPrisonerSentenceAndOffences(bookingId)
  }
}
