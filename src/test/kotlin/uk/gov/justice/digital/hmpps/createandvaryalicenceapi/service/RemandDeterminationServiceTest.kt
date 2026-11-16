package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing.PersonStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing.PersonStatusesResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing.RemandAndSentencingApiClient
import java.time.LocalDate

class RemandDeterminationServiceTest {
  private val remandAndSentencingApiClient = mock<RemandAndSentencingApiClient>()
  private val service = RemandDeterminationService(remandAndSentencingApiClient)

  @BeforeEach
  fun reset() {
    reset(remandAndSentencingApiClient)
  }

  @Nested
  inner class GetRemandBookingIds {

    @Test
    fun `returns the booking IDs of prisoners who are on remand`() {
      val onRemand = aPrisonerSearchResult.copy(prisonerNumber = "A1234AA", bookingId = "54321")
      val notOnRemand = aPrisonerSearchResult.copy(prisonerNumber = "A1234AB", bookingId = "54322")

      whenever(remandAndSentencingApiClient.getPersonStatuses(listOf("A1234AA", "A1234AB"))).thenReturn(
        PersonStatusesResponse(
          mapOf(
            "A1234AA" to PersonStatus(isOnRemand = true, isCurrentlyOnRecall = false),
            "A1234AB" to PersonStatus(isOnRemand = false, isCurrentlyOnRecall = false),
          ),
        ),
      )

      val remandBookingIds = service.getRemandBookingIds(listOf(onRemand, notOnRemand))

      assertThat(remandBookingIds).containsExactly(54321L)
    }

    @Test
    fun `returns an empty list when no prisoners are on remand`() {
      val prisoners = listOf(
        aPrisonerSearchResult.copy(prisonerNumber = "A1234AA", bookingId = "54321"),
        aPrisonerSearchResult.copy(prisonerNumber = "A1234AB", bookingId = "54322"),
      )

      whenever(remandAndSentencingApiClient.getPersonStatuses(any())).thenReturn(
        PersonStatusesResponse(
          mapOf(
            "A1234AA" to PersonStatus(isOnRemand = false, isCurrentlyOnRecall = false),
            "A1234AB" to PersonStatus(isOnRemand = false, isCurrentlyOnRecall = false),
          ),
        ),
      )

      val remandBookingIds = service.getRemandBookingIds(prisoners)

      assertThat(remandBookingIds).isEmpty()
    }

    @Test
    fun `excludes a remand prisoner with no booking id`() {
      val onRemandWithNoBookingId = aPrisonerSearchResult.copy(prisonerNumber = "A1234AA", bookingId = null)

      whenever(remandAndSentencingApiClient.getPersonStatuses(listOf("A1234AA"))).thenReturn(
        PersonStatusesResponse(mapOf("A1234AA" to PersonStatus(isOnRemand = true, isCurrentlyOnRecall = false))),
      )

      val remandBookingIds = service.getRemandBookingIds(listOf(onRemandWithNoBookingId))

      assertThat(remandBookingIds).isEmpty()
    }

    @Test
    fun `returns an empty list when the client returns a null response`() {
      val onRemand = aPrisonerSearchResult.copy(prisonerNumber = "A1234AA", bookingId = "54321")

      whenever(remandAndSentencingApiClient.getPersonStatuses(listOf("A1234AA"))).thenReturn(null)

      val remandBookingIds = service.getRemandBookingIds(listOf(onRemand))

      assertThat(remandBookingIds).isEmpty()
    }

    @Test
    fun `returns an empty list without calling the client when there are no prisoners`() {
      val remandBookingIds = service.getRemandBookingIds(emptyList())

      assertThat(remandBookingIds).isEmpty()
      verifyNoInteractions(remandAndSentencingApiClient)
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
    firstName = "Test",
    middleNames = null,
    lastName = "Person",
    dateOfBirth = LocalDate.parse("1985-01-01"),
    conditionalReleaseDateOverrideDate = null,
    sentenceStartDate = LocalDate.parse("2023-09-14"),
    sentenceExpiryDate = LocalDate.parse("2024-09-14"),
    topupSupervisionStartDate = null,
    croNumber = null,
  )
}
