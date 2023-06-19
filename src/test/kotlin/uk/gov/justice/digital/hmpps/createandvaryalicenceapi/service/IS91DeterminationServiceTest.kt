package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerOffenceHistory

class IS91DeterminationServiceTest {
  private val prisonApiClient = mock<PrisonApiClient>()
  private val service = IS91DeterminationService(prisonApiClient)

  @BeforeEach
  fun reset() {
    reset(
      prisonApiClient,
    )
  }

  @Test
  fun `Returns the booking IDs of licences with IS91-related primary result codes`() {
    val expectedIS91s = listOf(54321L, 54322L, 54323L, 54324L)
    val expectedNonIS91s = listOf(54325L, 54326)

    whenever(prisonApiClient.getOffenceHistories(expectedIS91s + expectedNonIS91s)).thenReturn(
      listOf(
        aPrisonerOffenceHistory.copy(bookingId = 54321, primaryResultCode = "5500"),
        aPrisonerOffenceHistory.copy(bookingId = 54322, primaryResultCode = "4022"),
        aPrisonerOffenceHistory.copy(bookingId = 54323, primaryResultCode = "3006"),
        aPrisonerOffenceHistory.copy(bookingId = 54324, primaryResultCode = "5502"),
        aPrisonerOffenceHistory.copy(bookingId = 54325, primaryResultCode = "1000"),
        aPrisonerOffenceHistory.copy(bookingId = 54326, primaryResultCode = "something_else"),
      ),
    )

    assert(service.getIS91AndExtraditionBookingIds(expectedIS91s + expectedNonIS91s) == expectedIS91s)
  }

  @Test
  fun `Returns the booking IDs of licences with IS91-related secondary result codes`() {
    val expectedIS91s = listOf(54321L, 54322L, 54323L, 54324L)
    val expectedNonIS91s = listOf(54325L, 54326)

    whenever(prisonApiClient.getOffenceHistories(expectedIS91s + expectedNonIS91s)).thenReturn(
      listOf(
        aPrisonerOffenceHistory.copy(bookingId = 54321, secondaryResultCode = "5500"),
        aPrisonerOffenceHistory.copy(bookingId = 54322, secondaryResultCode = "4022"),
        aPrisonerOffenceHistory.copy(bookingId = 54323, secondaryResultCode = "3006"),
        aPrisonerOffenceHistory.copy(bookingId = 54324, secondaryResultCode = "5502"),
        aPrisonerOffenceHistory.copy(bookingId = 54325, secondaryResultCode = "1000"),
        aPrisonerOffenceHistory.copy(bookingId = 54326, secondaryResultCode = "something_else"),
      ),
    )

    assert(service.getIS91AndExtraditionBookingIds(expectedIS91s + expectedNonIS91s) == expectedIS91s)
  }

  @Test
  fun `Returns the booking IDs of licences with IS91-related offence codes`() {
    val expectedIS91 = listOf(54321L)
    val expectedNonIS91 = listOf(54322L)

    whenever(prisonApiClient.getOffenceHistories(expectedIS91 + expectedNonIS91)).thenReturn(
      listOf(
        aPrisonerOffenceHistory.copy(bookingId = 54321, offenceCode = "IA99000-001N"),
        aPrisonerOffenceHistory.copy(bookingId = 54322, offenceCode = "something-else"),
      ),
    )

    assert(service.getIS91AndExtraditionBookingIds(expectedIS91 + expectedNonIS91) == expectedIS91)
  }

  private val aPrisonerOffenceHistory = PrisonerOffenceHistory(
    bookingId = 54321,
    offenceCode = "abc123",
    primaryResultCode = null,
    secondaryResultCode = null,
  )
}
