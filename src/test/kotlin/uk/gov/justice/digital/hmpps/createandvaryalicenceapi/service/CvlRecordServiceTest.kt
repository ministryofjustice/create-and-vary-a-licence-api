package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anIneligibleEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate

class CvlRecordServiceTest {
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val hdcService = mock<HdcService>()

  private val service = CvlRecordService(eligibilityService, releaseDateService, hdcService)

  @BeforeEach
  fun reset() {
    reset(eligibilityService, releaseDateService)
  }

  @Test
  fun `it builds the CvlRecords for a list of cases`() {
    val hdcStatuses = HdcStatuses(
      listOf(
        hdcPrisonerStatus().copy(
          bookingId = aPrisonerSearchPrisoner.bookingId?.toLong(),
          approvalStatus = HdcStatus.APPROVED.name,
        ),
        hdcPrisonerStatus().copy(
          bookingId = aPrisonerSearchPrisoner.bookingId?.plus(1)?.toLong(),
          approvalStatus = HdcStatus.NOT_A_HDC_RELEASE.name,
        ),
        hdcPrisonerStatus().copy(
          bookingId = aPrisonerSearchPrisoner.bookingId?.plus(2)?.toLong(),
          approvalStatus = HdcStatus.NOT_A_HDC_RELEASE.name,
        ),
      ),
    )

    whenever(
      eligibilityService.getEligibilityAssessments(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AB",
            bookingId = aPrisonerSearchPrisoner.bookingId?.plus(1),
          ),
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AC",
            bookingId = aPrisonerSearchPrisoner.bookingId?.plus(2),
          ),
        ),
        hdcStatuses,
      ),
    ).thenReturn(
      mapOf(
        aPrisonerSearchPrisoner.prisonerNumber to anEligibilityAssessment(),
        "A1234AB" to prrdEligibilityAssessment,
        "A1234AC" to anIneligibleEligibilityAssessment(),
      ),
    )
    whenever(
      releaseDateService.getLicenceStartDates(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AB",
            bookingId = aPrisonerSearchPrisoner.bookingId?.plus(1),
          ),
          aPrisonerSearchPrisoner.copy(
            prisonerNumber = "A1234AC",
            bookingId = aPrisonerSearchPrisoner.bookingId?.plus(2),
          ),
        ),
        mapOf(
          aPrisonerSearchPrisoner.prisonerNumber to EligibleKind.CRD,
          "A1234AB" to EligibleKind.FIXED_TERM,
          "A1234AC" to null,
        ),
      ),
    ).thenReturn(
      mapOf(
        aPrisonerSearchPrisoner.prisonerNumber to LocalDate.of(2021, 10, 22),
        "A1234AB" to LocalDate.of(2022, 11, 23),
        "A1234AC" to null,
      ),
    )
    whenever(releaseDateService.isTimeServed(anyOrNull())).thenReturn(false)
    whenever(releaseDateService.getHardStopDate(anyOrNull(), anyOrNull())).thenReturn(LocalDate.of(2023, 10, 12))
    whenever(releaseDateService.getHardStopWarningDate(anyOrNull(), anyOrNull())).thenReturn(LocalDate.of(2023, 10, 11))
    whenever(releaseDateService.isInHardStopPeriod(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.isEligibleForEarlyRelease(anyOrNull<SentenceDateHolder>())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(anyOrNull())).thenReturn(true)
    whenever(hdcService.getHdcStatus(any())).thenReturn(hdcStatuses)

    val results = service.getCvlRecords(
      listOf(
        aPrisonerSearchPrisoner,
        aPrisonerSearchPrisoner.copy(
          prisonerNumber = "A1234AB",
          bookingId = aPrisonerSearchPrisoner.bookingId?.plus(1),
        ),
        aPrisonerSearchPrisoner.copy(
          prisonerNumber = "A1234AC",
          bookingId = aPrisonerSearchPrisoner.bookingId?.plus(2),
        ),
      ),
    )
    assertThat(results).containsExactlyInAnyOrder(
      CvlRecord(
        nomisId = aPrisonerSearchPrisoner.prisonerNumber,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        isEligible = true,
        eligibleKind = EligibleKind.CRD,
        ineligibilityReasons = anEligibilityAssessment().ineligibilityReasons,
        creationKind = LicenceKind.HARD_STOP,
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isEligibleForEarlyRelease = true,
        isInHardStopPeriod = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
        licenceType = AP,
        isTimedOut = true,
        hdcStatus = HdcStatus.APPROVED,
      ),
      CvlRecord(
        nomisId = "A1234AB",
        licenceStartDate = LocalDate.of(2022, 11, 23),
        isEligible = true,
        eligibleKind = EligibleKind.FIXED_TERM,
        ineligibilityReasons = prrdEligibilityAssessment.ineligibilityReasons,
        creationKind = LicenceKind.HARD_STOP,
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isEligibleForEarlyRelease = true,
        isInHardStopPeriod = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
        licenceType = AP,
        isTimedOut = true,
        hdcStatus = HdcStatus.NOT_A_HDC_RELEASE,
      ),
      CvlRecord(
        nomisId = "A1234AC",
        licenceStartDate = null,
        isEligible = false,
        eligibleKind = null,
        ineligibilityReasons = anIneligibleEligibilityAssessment().ineligibilityReasons,
        creationKind = LicenceKind.HARD_STOP,
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isEligibleForEarlyRelease = true,
        isInHardStopPeriod = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
        licenceType = AP,
        isTimedOut = true,
        hdcStatus = HdcStatus.NOT_A_HDC_RELEASE,
      ),
    )
  }

  @Test
  fun `it builds the CvlRecord for an individual case`() {
    val hdcStatuses = HdcStatuses(
      listOf(
        hdcPrisonerStatus().copy(
          bookingId = aPrisonerSearchPrisoner.bookingId?.toLong(),
          approvalStatus = HdcStatus.NOT_A_HDC_RELEASE.name,
        ),
      ),
    )

    whenever(
      eligibilityService.getEligibilityAssessments(
        listOf(aPrisonerSearchPrisoner),
        hdcStatuses,
      ),
    ).thenReturn(mapOf(aPrisonerSearchPrisoner.prisonerNumber to anEligibilityAssessment()))

    whenever(hdcService.getHdcStatus(any())).thenReturn(hdcStatuses)

    whenever(
      releaseDateService.getLicenceStartDates(
        listOf(aPrisonerSearchPrisoner),
        mapOf(
          aPrisonerSearchPrisoner.prisonerNumber to EligibleKind.CRD,
        ),
      ),
    ).thenReturn(mapOf(aPrisonerSearchPrisoner.prisonerNumber to LocalDate.of(2021, 10, 22)))

    val result = service.getCvlRecord(aPrisonerSearchPrisoner)

    assertThat(result).isEqualTo(
      CvlRecord(
        nomisId = aPrisonerSearchPrisoner.prisonerNumber,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        isEligible = true,
        eligibleKind = EligibleKind.CRD,
        creationKind = LicenceKind.CRD,
        ineligibilityReasons = anEligibilityAssessment().ineligibilityReasons,
        isEligibleForEarlyRelease = false,
        isInHardStopPeriod = false,
        isDueToBeReleasedInTheNextTwoWorkingDays = false,
        licenceType = AP,
        isTimedOut = false,
        hdcStatus = HdcStatus.NOT_A_HDC_RELEASE,
      ),
    )
  }

  @Nested
  inner class IsTimedOutTest {
    @BeforeEach
    fun setup() {
      val hdcStatuses = HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(
            bookingId = prisonerSearchResult().bookingId?.toLong(),
            approvalStatus = HdcStatus.APPROVED.name,
          ),
        ),
      )
      whenever(
        eligibilityService.getEligibilityAssessments(
          any(),
          eq(hdcStatuses),
        ),
      ).thenReturn(
        mapOf(aPrisonerSearchPrisoner.prisonerNumber to anEligibilityAssessment()),
      )
      whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(
        mapOf(aPrisonerSearchPrisoner.prisonerNumber to LocalDate.of(2021, 10, 22)),
      )
      whenever(hdcService.getHdcStatus(any())).thenReturn(hdcStatuses)
    }

    @Test
    fun `should be true when prisoner is time served`() {
      whenever(releaseDateService.isTimeServed(anyOrNull())).thenReturn(true)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(false)

      val cvlRecord = service.getCvlRecord(aPrisonerSearchPrisoner)

      assertThat(cvlRecord.isTimedOut).isTrue()
    }

    @Test
    fun `should be true when in hard stop period`() {
      whenever(releaseDateService.isTimeServed(anyOrNull())).thenReturn(false)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(true)

      val cvlRecord = service.getCvlRecord(aPrisonerSearchPrisoner)

      assertThat(cvlRecord.isTimedOut).isTrue()
    }

    @Test
    fun `should be false when not time served and not in hard stop period`() {
      whenever(releaseDateService.isTimeServed(anyOrNull())).thenReturn(false)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(false)

      val cvlRecord = service.getCvlRecord(aPrisonerSearchPrisoner)

      assertThat(cvlRecord.isTimedOut).isFalse()
    }
  }

  @Nested
  inner class CreationKindTest {
    @BeforeEach
    fun setup() {
      val hdcStatuses = HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(
            bookingId = prisonerSearchResult().bookingId?.toLong(),
            approvalStatus = HdcStatus.APPROVED.name,
          ),
        ),
      )
      whenever(eligibilityService.getEligibilityAssessments(any(), eq(hdcStatuses))).thenReturn(
        mapOf(aPrisonerSearchPrisoner.prisonerNumber to anEligibilityAssessment()),
      )
      whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(
        mapOf(aPrisonerSearchPrisoner.prisonerNumber to LocalDate.of(2021, 10, 22)),
      )
      whenever(hdcService.getHdcStatus(any())).thenReturn(hdcStatuses)
    }

    @Test
    fun `should return TIME_SERVED when prisoner is time served`() {
      whenever(releaseDateService.isTimeServed(any())).thenReturn(true)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(false)

      val cvlRecord = service.getCvlRecord(aPrisonerSearchPrisoner)

      assertThat(cvlRecord.creationKind).isEqualTo(LicenceKind.TIME_SERVED)
    }

    @Test
    fun `should return HARD_STOP when in hard stop period`() {
      whenever(releaseDateService.isTimeServed(any())).thenReturn(false)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(true)

      val cvlRecord = service.getCvlRecord(aPrisonerSearchPrisoner)

      assertThat(cvlRecord.creationKind).isEqualTo(LicenceKind.HARD_STOP)
    }

    @Test
    fun `should return eligibleKind when not time served and not in hard stop period`() {
      whenever(releaseDateService.isTimeServed(any())).thenReturn(false)
      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull(), anyOrNull())).thenReturn(false)

      val cvlRecord = service.getCvlRecord(aPrisonerSearchPrisoner)

      assertThat(cvlRecord.creationKind).isEqualTo(cvlRecord.eligibleKind?.licenceKind)
    }
  }

  private val aPrisonerSearchPrisoner = prisonerSearchResult()
  private val prrdEligibilityAssessment = anEligibilityAssessment().copy(
    crdIneligibilityReasons = listOf("Some reason"),
    hdcIneligibilityReasons = listOf("Some reason"),
    eligibleKind = EligibleKind.FIXED_TERM,
  )
}
