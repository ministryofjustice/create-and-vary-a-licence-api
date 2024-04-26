package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class CaseloadServiceTest {
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = CaseloadService(prisonerSearchApiClient, releaseDateService)

  @BeforeEach
  fun reset() {
    reset(prisonerSearchApiClient)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisonerSearchResult()))
    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any())).thenReturn(listOf(prisonerSearchResult()))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 10, 12))
    whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 10, 11))
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
    whenever(releaseDateService.isEligibleForEarlyRelease(any<SentenceDateHolder>())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)
  }

  @Test
  fun searchPrisonersByNomisIds() {
    val response = service.getPrisonersByNumber(listOf("A1234AA"))

    assertThat(response).containsExactly(
      CaseloadItem(
        cvl = CvlFields(
          licenceType = LicenceType.AP,
          hardStopDate = LocalDate.of(2023, 10, 12),
          hardStopWarningDate = LocalDate.of(2023, 10, 11),
          isInHardStopPeriod = true,
          isEligibleForEarlyRelease = true,
          isDueForEarlyRelease = true,
          isDueToBeReleasedInTheNextTwoWorkingDays = true,
        ),
        prisoner = Prisoner(
          prisonerNumber = "A1234AA",
          pncNumber = null,
          croNumber = null,
          bookingId = "123456",
          bookNumber = "12345A",
          firstName = "Bob",
          middleNames = null,
          lastName = "Mortimar",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          status = "ACTIVE IN",
          inOutStatus = null,
          prisonId = "MDI",
          prisonName = null,
          locationDescription = "HMP Moorland",
          legalStatus = "SENTENCED",
          imprisonmentStatus = null,
          imprisonmentStatusDescription = null,
          mostSeriousOffence = "Robbery",
          recall = false,
          indeterminateSentence = false,
          sentenceStartDate = LocalDate.of(2018, 10, 22),
          releaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
          sentenceExpiryDate = LocalDate.of(2021, 10, 22),
          licenceExpiryDate = LocalDate.of(2021, 10, 22),
          homeDetentionCurfewEligibilityDate = null,
          homeDetentionCurfewActualDate = null,
          homeDetentionCurfewEndDate = null,
          topupSupervisionStartDate = null,
          topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
          paroleEligibilityDate = null,
          postRecallReleaseDate = null,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          actualParoleDate = null,
          releaseOnTemporaryLicenceDate = null,
        ),
      ),
    )
    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf("A1234AA"))
  }

  @Test
  fun getPrisoner() {
    val response = service.getPrisoner("A1234AA")
    assertThat(response).isEqualTo(
      CaseloadItem(
        cvl = CvlFields(
          licenceType = LicenceType.AP,
          hardStopDate = LocalDate.of(2023, 10, 12),
          hardStopWarningDate = LocalDate.of(2023, 10, 11),
          isInHardStopPeriod = true,
          isDueForEarlyRelease = true,
          isEligibleForEarlyRelease = true,
          isDueToBeReleasedInTheNextTwoWorkingDays = true,
        ),
        prisoner = Prisoner(
          prisonerNumber = "A1234AA",
          pncNumber = null,
          croNumber = null,
          bookingId = "123456",
          bookNumber = "12345A",
          firstName = "Bob",
          middleNames = null,
          lastName = "Mortimar",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          status = "ACTIVE IN",
          inOutStatus = null,
          prisonId = "MDI",
          prisonName = null,
          locationDescription = "HMP Moorland",
          legalStatus = "SENTENCED",
          imprisonmentStatus = null,
          imprisonmentStatusDescription = null,
          mostSeriousOffence = "Robbery",
          recall = false,
          indeterminateSentence = false,
          sentenceStartDate = LocalDate.of(2018, 10, 22),
          releaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
          sentenceExpiryDate = LocalDate.of(2021, 10, 22),
          licenceExpiryDate = LocalDate.of(2021, 10, 22),
          homeDetentionCurfewEligibilityDate = null,
          homeDetentionCurfewActualDate = null,
          homeDetentionCurfewEndDate = null,
          topupSupervisionStartDate = null,
          topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
          paroleEligibilityDate = null,
          postRecallReleaseDate = null,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          actualParoleDate = null,
          releaseOnTemporaryLicenceDate = null,
        ),
      ),
    )
    verify(prisonerSearchApiClient).searchPrisonersByNomisIds(listOf("A1234AA"))
  }

  @Test
  fun getPrisonerFailsToFind() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(emptyList())

    assertThatThrownBy { service.getPrisoner("A1234AA") }.isInstanceOf(EntityNotFoundException::class.java).hasMessage("A1234AA")
  }

  @Test
  fun getPrisonersByReleaseDate() {
    val response = service.getPrisonersByReleaseDate(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 4), setOf("MDI"))
    assertThat(response).containsExactly(
      CaseloadItem(
        cvl = CvlFields(
          licenceType = LicenceType.AP,
          hardStopDate = LocalDate.of(2023, 10, 12),
          hardStopWarningDate = LocalDate.of(2023, 10, 11),
          isInHardStopPeriod = true,
          isDueForEarlyRelease = true,
          isEligibleForEarlyRelease = true,
          isDueToBeReleasedInTheNextTwoWorkingDays = true,
        ),
        prisoner = Prisoner(
          prisonerNumber = "A1234AA",
          pncNumber = null,
          croNumber = null,
          bookingId = "123456",
          bookNumber = "12345A",
          firstName = "Bob",
          middleNames = null,
          lastName = "Mortimar",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          status = "ACTIVE IN",
          inOutStatus = null,
          prisonId = "MDI",
          locationDescription = "HMP Moorland",
          prisonName = null,
          legalStatus = "SENTENCED",
          imprisonmentStatus = null,
          imprisonmentStatusDescription = null,
          mostSeriousOffence = "Robbery",
          recall = false,
          indeterminateSentence = false,
          sentenceStartDate = LocalDate.of(2018, 10, 22),
          releaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
          sentenceExpiryDate = LocalDate.of(2021, 10, 22),
          licenceExpiryDate = LocalDate.of(2021, 10, 22),
          homeDetentionCurfewEligibilityDate = null,
          homeDetentionCurfewActualDate = null,
          homeDetentionCurfewEndDate = null,
          topupSupervisionStartDate = null,
          topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
          paroleEligibilityDate = null,
          postRecallReleaseDate = null,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          actualParoleDate = null,
          releaseOnTemporaryLicenceDate = null,
        ),
      ),
    )
    verify(prisonerSearchApiClient).searchPrisonersByReleaseDate(LocalDate.of(2023, 1, 2), LocalDate.of(2023, 1, 4), setOf("MDI"))
  }
}
