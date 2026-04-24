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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aCvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.corePersonRecord.CorePersonRecordApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class CaseServiceTest {
  private val corePersonRecordApiClient = mock<CorePersonRecordApiClient>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val cvlRecordService = mock<CvlRecordService>()
  private val deliusApiClient = mock<DeliusApiClient>()

  private val service =
    CaseService(corePersonRecordApiClient, cvlRecordService, deliusApiClient, prisonerSearchApiClient)

  @BeforeEach
  fun reset() {
    reset(corePersonRecordApiClient, prisonerSearchApiClient, cvlRecordService, deliusApiClient)
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(any())).thenReturn(listOf(prisonerSearchResult()))
    whenever(
      prisonerSearchApiClient.searchPrisonersByReleaseDate(
        any(),
        any(),
        any(),
        anyOrNull(),
        anyOrNull(),
      ),
    ).thenReturn(PageImpl(listOf(prisonerSearchResult())))
  }

  @Test
  fun getPrisoner() {
    whenever(deliusApiClient.getOffenderManager(any())).thenReturn(offenderManager())
    whenever(cvlRecordService.getCvlRecord(any())).thenReturn(
      aCvlRecord(
        eligibleKind = EligibleKind.CRD,
        hardStopKind = LicenceKind.HARD_STOP,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isInHardStopPeriod = true,
        isEligibleForEarlyRelease = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
      ),
    )

    val response = service.getPrisoner("A1234AA")
    assertThat(response).isEqualTo(
      PrisonerWithCvlFields(
        cvl = CvlFields(
          licenceType = LicenceType.AP,
          hardStopDate = LocalDate.of(2023, 10, 12),
          hardStopWarningDate = LocalDate.of(2023, 10, 11),
          isInHardStopPeriod = true,
          isEligibleForEarlyRelease = true,
          isDueToBeReleasedInTheNextTwoWorkingDays = true,
          licenceStartDate = LocalDate.of(2021, 10, 22),
          licenceKind = LicenceKind.CRD,
          hardStopKind = LicenceKind.HARD_STOP,
          eligibleKind = EligibleKind.CRD,
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
  fun `should get a probation case`() {
    val prisonNumber = "A1234AA"
    val deliusRecord =
      ProbationCase(crn = "X123456", nomisId = prisonNumber, croNumber = "43792/24M", pncNumber = "2019/123445")

    whenever(deliusApiClient.getProbationCase(prisonNumber)).thenReturn(deliusRecord)

    val probationCase = service.getProbationCase(prisonNumber)
    assertThat(probationCase).isEqualTo(deliusRecord)
  }
}
