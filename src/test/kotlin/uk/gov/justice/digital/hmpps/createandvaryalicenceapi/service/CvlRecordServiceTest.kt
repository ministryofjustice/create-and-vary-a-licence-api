package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate

class CvlRecordServiceTest {
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = CvlRecordService(eligibilityService, releaseDateService)

  @BeforeEach
  fun reset() {
    reset(eligibilityService, releaseDateService)
  }

  @Test
  fun `it builds the CvlRecords for a list of cases`() {
    whenever(
      eligibilityService.getEligibilityAssessments(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AB"),
          aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AC"),
        ),
      ),
    ).thenReturn(
      mapOf(
        aPrisonerSearchPrisoner.prisonerNumber to anEligibilityAssessment(),
        "A1234AB" to prrdEligibilityAssessment,
        "A1234AC" to ineligibleEligibilityAssessment,
      ),
    )
    whenever(
      releaseDateService.getLicenceStartDates(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AB"),
          aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AC"),
        ),
        mapOf(
          aPrisonerSearchPrisoner.prisonerNumber to LicenceKind.CRD,
          "A1234AB" to LicenceKind.PRRD,
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
    whenever(releaseDateService.getHardStopKind(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(LicenceKind.HARD_STOP)
    whenever(releaseDateService.getHardStopDate(anyOrNull(), anyOrNull())).thenReturn(LocalDate.of(2023, 10, 12))
    whenever(releaseDateService.getHardStopWarningDate(anyOrNull(), anyOrNull())).thenReturn(LocalDate.of(2023, 10, 11))
    whenever(releaseDateService.isInHardStopPeriod(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(true)
    whenever(releaseDateService.isEligibleForEarlyRelease(anyOrNull<SentenceDateHolder>())).thenReturn(true)
    whenever(releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(anyOrNull())).thenReturn(true)

    val results = service.getCvlRecords(
      listOf(
        aPrisonerSearchPrisoner,
        aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AB"),
        aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AC"),
      ),
    )
    assertThat(results).containsExactlyInAnyOrder(
      CvlRecord(
        nomisId = aPrisonerSearchPrisoner.prisonerNumber,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        isEligible = true,
        eligibleKind = LicenceKind.CRD,
        ineligibilityReasons = anEligibilityAssessment().ineligibilityReasons,
        hardStopKind = LicenceKind.HARD_STOP,
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isEligibleForEarlyRelease = true,
        isInHardStopPeriod = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
        licenceType = AP,
      ),
      CvlRecord(
        nomisId = "A1234AB",
        licenceStartDate = LocalDate.of(2022, 11, 23),
        isEligible = true,
        eligibleKind = LicenceKind.PRRD,
        ineligibilityReasons = prrdEligibilityAssessment.ineligibilityReasons,
        hardStopKind = LicenceKind.HARD_STOP,
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isEligibleForEarlyRelease = true,
        isInHardStopPeriod = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
        licenceType = AP,
      ),
      CvlRecord(
        nomisId = "A1234AC",
        licenceStartDate = null,
        isEligible = false,
        eligibleKind = null,
        ineligibilityReasons = ineligibleEligibilityAssessment.ineligibilityReasons,
        hardStopKind = LicenceKind.HARD_STOP,
        hardStopDate = LocalDate.of(2023, 10, 12),
        hardStopWarningDate = LocalDate.of(2023, 10, 11),
        isEligibleForEarlyRelease = true,
        isInHardStopPeriod = true,
        isDueToBeReleasedInTheNextTwoWorkingDays = true,
        licenceType = AP,
      ),
    )
  }

  @Test
  fun `it builds the CvlRecord for an individual case`() {
    whenever(
      eligibilityService.getEligibilityAssessments(
        listOf(aPrisonerSearchPrisoner),
      ),
    ).thenReturn(mapOf(aPrisonerSearchPrisoner.prisonerNumber to anEligibilityAssessment()))

    whenever(
      releaseDateService.getLicenceStartDates(
        listOf(aPrisonerSearchPrisoner),
        mapOf(
          aPrisonerSearchPrisoner.prisonerNumber to LicenceKind.CRD,
        ),
      ),
    ).thenReturn(mapOf(aPrisonerSearchPrisoner.prisonerNumber to LocalDate.of(2021, 10, 22)))

    val result = service.getCvlRecord(aPrisonerSearchPrisoner)

    assertThat(result).isEqualTo(
      CvlRecord(
        nomisId = aPrisonerSearchPrisoner.prisonerNumber,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        isEligible = true,
        eligibleKind = LicenceKind.CRD,
        ineligibilityReasons = anEligibilityAssessment().ineligibilityReasons,
        isEligibleForEarlyRelease = false,
        isInHardStopPeriod = false,
        isDueToBeReleasedInTheNextTwoWorkingDays = false,
        licenceType = AP,
      ),
    )
  }

  @Nested
  inner class LicenceTypeTest {
    @BeforeEach
    fun reset() {
      whenever(eligibilityService.getEligibilityAssessments(any())).thenReturn(
        mapOf(prisonerSearchResult().prisonerNumber to anEligibilityAssessment()),
      )
      whenever(releaseDateService.isReleaseAtLed(any(), any())).thenReturn(false)
    }

    @Test
    fun `should default to AP`() {
      val nomisRecord =
        prisonerSearchResult().copy(licenceExpiryDate = null, topupSupervisionExpiryDate = null)
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(AP)
    }

    @Test
    fun `should be PSS when TUSED is defined and LED is undefined`() {
      val nomisRecord = prisonerSearchResult()
        .copy(licenceExpiryDate = null, topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22))
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.PSS)
    }

    @Test
    fun `should be AP when LED is defined and TUSED is undefined`() {
      val nomisRecord = prisonerSearchResult()
        .copy(licenceExpiryDate = LocalDate.of(2021, 10, 22), topupSupervisionExpiryDate = null)
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(AP)
    }

    @Test
    fun `should be AP when TUSED is before LED`() {
      val nomisRecord = prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 21),
      )
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(AP)
    }

    @Test
    fun `should be AP when TUSED is equal to LED`() {
      val nomisRecord = prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      )
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(AP)
    }

    @Test
    fun `should be AP_PSS when TUSED is after LED`() {
      val nomisRecord = prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 23),
      )
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.AP_PSS)
    }

    @Test
    fun `AP_PSS recall cases are PSS-only if the licence start date is equal to the LED`() {
      whenever(eligibilityService.getEligibilityAssessments(any())).thenReturn(
        mapOf(prisonerSearchResult().prisonerNumber to prrdEligibilityAssessment),
      )
      whenever(releaseDateService.getLicenceStartDates(any(), any())).thenReturn(
        mapOf(prisonerSearchResult().prisonerNumber to LocalDate.of(2021, 10, 22)),
      )
      whenever(releaseDateService.isReleaseAtLed(any(), any())).thenReturn(true)

      val nomisRecord = prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 23),
      )
      val cvlRecord = service.getCvlRecord(nomisRecord)
      assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.PSS)
    }

    private val prrdEligibilityAssessment = anEligibilityAssessment().copy(
      crdIneligibilityReasons = listOf("Some reason"),
      eligibleKind = LicenceKind.PRRD,
    )
  }

  private val aPrisonerSearchPrisoner = prisonerSearchResult()
  private val prrdEligibilityAssessment = anEligibilityAssessment().copy(
    crdIneligibilityReasons = listOf("Some reason"),
    eligibleKind = LicenceKind.PRRD,
  )
  private val ineligibleEligibilityAssessment = anEligibilityAssessment().copy(
    genericIneligibilityReasons = listOf("Some reason"),
    isEligible = false,
    eligibleKind = null,
  )
}
