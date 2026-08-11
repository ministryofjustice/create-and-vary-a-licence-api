package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.CourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.RemandCourtEvents
import java.time.LocalDate

class RemandDeterminationServiceTest {
  private val prisonApiClient = mock<PrisonApiClient>()
  private val service = RemandDeterminationService(prisonApiClient)

  @BeforeEach
  fun reset() {
    reset(prisonApiClient)
  }

  @Nested
  inner class GetRemandBookingIds {
    @Test
    fun `returns the booking IDs of licences with a remand court outcome code`() {
      val expectedRemands = listOf(54321L, 54322L)

      whenever(prisonApiClient.getCourtEventOutcomes(listOf(54321L, 54322L), RemandCourtEvents.getRemandCourtCodes())).thenReturn(
        listOf(
          CourtEventOutcome(bookingId = 54321L, eventId = 1L, outcomeReasonCode = "4531"),
          CourtEventOutcome(bookingId = 54322L, eventId = 2L, outcomeReasonCode = "5601"),
        ),
      )

      val remandBookingIds = service.getRemandBookingIds(
        listOf(
          aPrisonerSearchResult.copy(bookingId = "54321"),
          aPrisonerSearchResult.copy(bookingId = "54322"),
        ),
      )

      assertThat(remandBookingIds).containsExactlyInAnyOrderElementsOf(expectedRemands)
    }

    @Test
    fun `returns an empty list if there are no booking IDs`() {
      val remandBookingIds = service.getRemandBookingIds(
        listOf(aPrisonerSearchResult.copy(bookingId = null)),
      )

      assertThat(remandBookingIds).isEmpty()
    }
  }

  @Nested
  inner class IsRemandCase {
    @ParameterizedTest(name = "returns true for {0}")
    @EnumSource(RemandCourtEvents::class)
    fun `returns true for a remand court outcome code`(outcomeCode: RemandCourtEvents) {
      whenever(prisonApiClient.getCourtEventOutcomes(listOf(54322L), RemandCourtEvents.getRemandCourtCodes())).thenReturn(
        listOf(CourtEventOutcome(bookingId = 54322L, eventId = 1L, outcomeReasonCode = outcomeCode.code)),
      )

      val prisoner = aPrisonerSearchResult.copy(bookingId = "54322")
      assertThat(service.isRemandCase(prisoner)).isTrue()
    }

    @Test
    fun `returns false for a case with any other outcome code`() {
      whenever(prisonApiClient.getCourtEventOutcomes(listOf(54322L), RemandCourtEvents.getRemandCourtCodes())).thenReturn(emptyList())

      val prisoner = aPrisonerSearchResult.copy(bookingId = "54322")
      assertThat(service.isRemandCase(prisoner)).isFalse()
    }

    @Test
    fun `returns false if the case has no booking ID`() {
      val prisoner = aPrisonerSearchResult.copy(bookingId = null)
      assertThat(service.isRemandCase(prisoner)).isFalse()
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
