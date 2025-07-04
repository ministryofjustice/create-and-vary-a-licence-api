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
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class CaseloadServiceTest {
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val service =
    CaseloadService(prisonerSearchApiClient, releaseDateService)

  @BeforeEach
  fun reset() {
    reset(prisonerSearchApiClient, releaseDateService)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisonerSearchResult()))
    whenever(
      prisonerSearchApiClient.searchPrisonersByReleaseDate(
        any(),
        any(),
        any(),
        anyOrNull(),
      ),
    ).thenReturn(PageImpl(listOf(prisonerSearchResult())))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.of(2023, 10, 12))
    whenever(releaseDateService.getHardStopWarningDate(any())).thenReturn(LocalDate.of(2023, 10, 11))
    whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.isDueForEarlyRelease(any())).thenReturn(true)
    whenever(releaseDateService.isEligibleForEarlyRelease(any<SentenceDateHolder>())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(any())).thenReturn(true)
    whenever(releaseDateService.getLicenceStartDates(any())).thenReturn(
      mapOf(
        "A1234AA" to LocalDate.of(2021, 10, 22),
      ),
    )
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
          licenceStartDate = LocalDate.of(2021, 10, 22),
        ),
        prisoner = Prisoner(
          prisonerNumber = "A1234AA",
          pncNumber = null,
          croNumber = null,
          bookingId = "123456",
          bookNumber = "12345A",
          firstName = "A",
          middleNames = null,
          lastName = "Prisoner",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          status = "ACTIVE IN",
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
          licenceStartDate = LocalDate.of(2021, 10, 22),
        ),
        prisoner = Prisoner(
          prisonerNumber = "A1234AA",
          pncNumber = null,
          croNumber = null,
          bookingId = "123456",
          bookNumber = "12345A",
          firstName = "A",
          middleNames = null,
          lastName = "Prisoner",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          status = "ACTIVE IN",
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

    assertThatThrownBy { service.getPrisoner("A1234AA") }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("A1234AA")
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
          licenceStartDate = LocalDate.of(2021, 10, 22),
        ),
        prisoner = Prisoner(
          prisonerNumber = "A1234AA",
          pncNumber = null,
          croNumber = null,
          bookingId = "123456",
          bookNumber = "12345A",
          firstName = "A",
          middleNames = null,
          lastName = "Prisoner",
          dateOfBirth = LocalDate.of(1985, 12, 28),
          status = "ACTIVE IN",
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
    verify(prisonerSearchApiClient).searchPrisonersByReleaseDate(
      LocalDate.of(2023, 1, 2),
      LocalDate.of(2023, 1, 4),
      setOf("MDI"),
    )
  }

  @Test
  fun `should determine a licence with PRRD in future and after CRD in the past as a recall licence`() {
    val prisoner =
      prisonerSearchResult(
        conditionalReleaseDate = LocalDate.now().minusDays(1),
        postRecallReleaseDate = LocalDate.now().plusDays(1),
      )

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.PRRD)
  }

  @Test
  fun `should determine a licence with PRRD in future and before CRD as a CRD licence`() {
    val prisoner =
      prisonerSearchResult(
        postRecallReleaseDate = LocalDate.now().plusDays(1),
        conditionalReleaseDate = LocalDate.now().plusDays(2),
      )
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(null)

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.CRD)
  }

  @Test
  fun `should determine a licence with null PRRD and a hard stop date of today as a hard stop licence`() {
    val prisoner = prisonerSearchResult(conditionalReleaseDate = LocalDate.now().plusDays(2))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(LocalDate.now())

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.HARD_STOP)
  }

  @Test
  fun `should determine a licence with null PRRD and hard stop date in future as a CRD licence`() {
    val prisoner = prisonerSearchResult(conditionalReleaseDate = LocalDate.now().plusDays(2))
    whenever(releaseDateService.getHardStopDate(any())).thenReturn(null)

    val licenceKind = service.determineLicenceKind(prisoner)

    assertThat(licenceKind).isEqualTo(LicenceKind.CRD)
  }
}
