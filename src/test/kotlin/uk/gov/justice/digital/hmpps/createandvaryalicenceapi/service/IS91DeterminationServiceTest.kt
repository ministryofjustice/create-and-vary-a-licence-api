package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
  private val resultCodes = listOf("3006", "4022", "5500", "5502")

  @BeforeEach
  fun reset() {
    reset(
      prisonApiClient,
    )
  }

  @Nested
  inner class GetIS91AndExtraditionBookingIds {

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
  }

  @Nested
  inner class IsIS91Case {
    @Test
    fun `Returns true for a case with an illegal immigrant offence code`() {
      val prisoner = aPrisonerSearchResult.copy(bookingId = "54322", mostSeriousOffence = "ILLEGAL IMMIGRANT/DETAINEE")
      assertThat(service.isIS91Case(prisoner)).isTrue()
    }

    @Test
    fun `Returns false for a case with any other offence code`() {
      val prisoner = aPrisonerSearchResult.copy(bookingId = "54322", mostSeriousOffence = "OFFENCE1")
      assertThat(service.isIS91Case(prisoner)).isFalse()
    }

    @ParameterizedTest(name = "returns true for {0}")
    @ValueSource(strings = ["3006", "4022", "5500", "5502"])
    fun `Returns true for a case with an IS91 related court outcome code`(outcomeCode: String) {
      whenever(prisonApiClient.getCourtEventOutcomes(listOf(54322), resultCodes)).thenReturn(
        listOf(CourtEventOutcome(bookingId = 43566, eventId = 1, outcomeReasonCode = outcomeCode)),
      )
      val prisoner = aPrisonerSearchResult.copy(bookingId = "54322")
      assertThat(service.isIS91Case(prisoner)).isTrue()
    }

    @Test
    fun `Returns false if the case has no booking ID`() {
      val prisoner = aPrisonerSearchResult.copy(bookingId = null)
      assertThat(service.isIS91Case(prisoner)).isFalse()
    }
  }

  private val aPrisonerSearchResult = PrisonerSearchPrisoner(
    prisonerNumber = "A1234AA",
    bookingId = "1234567",
    status = "ACTIVE IN",
    mostSeriousOffence = "Robbery",
    licenceExpiryDate = LocalDate.parse("2024-09-14"),
    topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
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
    locationDescription = "HMP Moorland",
    bookNumber = "12345A",
    firstName = "Jane",
    middleNames = null,
    lastName = "Doe",
    dateOfBirth = LocalDate.parse("1985-01-01"),
    conditionalReleaseDateOverrideDate = null,
    sentenceStartDate = LocalDate.parse("2023-09-14"),
    sentenceExpiryDate = LocalDate.parse("2024-09-14"),
    topupSupervisionStartDate = null,
    croNumber = null,
  )
}
