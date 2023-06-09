package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerOffenceHistory

class PrisonerApiFunctionsTest {

  @Test
  fun `Returns the booking IDs of licences with IS91-related primary result codes`() {
    val expectedIS91s = listOf(
      aPrisonerOffenceHistory.copy(primaryResultCode = "5500"),
      aPrisonerOffenceHistory.copy(bookingId = 54322, primaryResultCode = "4022"),
      aPrisonerOffenceHistory.copy(bookingId = 54323, primaryResultCode = "3006"),
      aPrisonerOffenceHistory.copy(bookingId = 54324, primaryResultCode = "5502")
    )
    val expectedNonIS91s = listOf(aPrisonerOffenceHistory.copy(bookingId = 54325), aPrisonerOffenceHistory.copy(bookingId = 54326))

    assert(getIS91AndExtraditionBookingIds(expectedIS91s + expectedNonIS91s) == expectedIS91s.map { it.bookingId })
  }

  @Test
  fun `Returns the booking IDs of licences with IS91-related secondary result codes`() {
    val expectedIS91s = listOf(
      aPrisonerOffenceHistory.copy(secondaryResultCode = "5500"),
      aPrisonerOffenceHistory.copy(bookingId = 54322, secondaryResultCode = "4022"),
      aPrisonerOffenceHistory.copy(bookingId = 54323, secondaryResultCode = "3006"),
      aPrisonerOffenceHistory.copy(bookingId = 54324, secondaryResultCode = "5502")
    )
    val expectedNonIS91s = listOf(aPrisonerOffenceHistory.copy(bookingId = 54325), aPrisonerOffenceHistory.copy(bookingId = 54326))

    assert(getIS91AndExtraditionBookingIds(expectedIS91s + expectedNonIS91s) == expectedIS91s.map { it.bookingId })
  }

  @Test
  fun `Returns the booking IDs of licences with IS91-related offence codes`() {
    val expectedIS91 = aPrisonerOffenceHistory.copy(offenceCode = "IA99000-001N")
    val expectedNonIS91 = aPrisonerOffenceHistory.copy(bookingId = 54322)

    assert(getIS91AndExtraditionBookingIds(listOf(expectedIS91, expectedNonIS91)) == listOf(expectedIS91.bookingId))
  }

  private val aPrisonerOffenceHistory = PrisonerOffenceHistory(
    bookingId = 54321,
    offenceCode = "abc123",
    primaryResultCode = null,
    secondaryResultCode = null
  )
}
