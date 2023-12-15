package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.CourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.LocalDate

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
  fun `Returns the booking IDs of licences with an illegal immigrant offence code`() {
    val expectedIS91s = listOf(54321L, 54322L)

    val prisoners =
      listOf(
        aPrisonerSearchResult.copy(bookingId = "54325", mostSeriousOffence = "offence 1"),
        aPrisonerSearchResult.copy(bookingId = "325653", mostSeriousOffence = "offence 2"),
        aPrisonerSearchResult.copy(bookingId = "54326", mostSeriousOffence = "offence 3"),
        aPrisonerSearchResult.copy(bookingId = "54322", mostSeriousOffence = "ILLEGAL IMMIGRANT/DETAINEE"),
        aPrisonerSearchResult.copy(bookingId = "93564", mostSeriousOffence = "offence 4"),
        aPrisonerSearchResult.copy(bookingId = "54321", mostSeriousOffence = "ILLEGAL IMMIGRANT/DETAINEE"),
      )

    val is91BookingIds = service.getIS91AndExtraditionBookingIds(prisoners)
    assertThat(is91BookingIds).containsExactlyInAnyOrderElementsOf(expectedIS91s)
  }

  @Test
  fun `Returns the booking IDs of prisoners with IS91 related court event outcome codes`() {
    val expectedIS91s = listOf(84379L, 902322L)
    val expectedNonIS91s = listOf(43566L, 843793L, 5387L)

    val prisoners = listOf(
      aPrisonerSearchResult.copy(bookingId = "84379", mostSeriousOffence = "offence 1"),
      aPrisonerSearchResult.copy(bookingId = "902322", mostSeriousOffence = "offence 2"),
      aPrisonerSearchResult.copy(bookingId = "43566", mostSeriousOffence = "offence 3"),
      aPrisonerSearchResult.copy(bookingId = "843793", mostSeriousOffence = "offence 4"),
      aPrisonerSearchResult.copy(bookingId = "5387", mostSeriousOffence = "offence 5"),
    )

    whenever(prisonApiClient.getCourtEventOutcomes(expectedIS91s + expectedNonIS91s)).thenReturn(
      listOf(
        CourtEventOutcome(bookingId = 43566, eventId = 1, outcomeReasonCode = "3692"),
        CourtEventOutcome(bookingId = 84379, eventId = 2, outcomeReasonCode = "5500"),
        CourtEventOutcome(bookingId = 843793, eventId = 3, outcomeReasonCode = "8922"),
        CourtEventOutcome(bookingId = 902322, eventId = 4, outcomeReasonCode = "4022"),
        CourtEventOutcome(bookingId = 5387, eventId = 5, outcomeReasonCode = null),
      ),
    )

    assert(service.getIS91AndExtraditionBookingIds(prisoners) == expectedIS91s)
  }

  private val aPrisonerSearchResult = PrisonerSearchPrisoner(
    prisonerNumber = "A1234AA",
    bookingId = "1234567",
    status = "ACTIVE IN",
    mostSeriousOffence = "Robbery",
    licenceExpiryDate = LocalDate.parse("2024-09-14"),
    topUpSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
    homeDetentionCurfewEligibilityDate = null,
    releaseDate = LocalDate.parse("2023-09-14"),
    confirmedReleaseDate = LocalDate.parse("2023-09-14"),
    conditionalReleaseDate = LocalDate.parse("2023-09-14"),
    paroleEligibilityDate = null,
    actualParoleDate = null,
    postRecallReleaseDate = null,
    legalStatus = "SENTENCED",
    indeterminateSentence = false,
    recall = false,
    prisonId = "ABC",
    bookNumber = "12345A",
    firstName = "Jane",
    middleNames = null,
    lastName = "Doe",
    dateOfBirth = LocalDate.parse("1985-01-01"),
    conditionalReleaseDateOverrideDate = null,
    sentenceStartDate = LocalDate.parse("2023-09-14"),
    sentenceExpiryDate = LocalDate.parse("2024-09-14"),
    topUpSupervisionStartDate = null,
    croNumber = null,
  )
}
