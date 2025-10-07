package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
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
  fun `it builds the CvlCaseDtos for a list of cases`() {
    whenever(
      eligibilityService.getEligibilityAssessments(
        listOf(
          aPrisonerSearchPrisoner,
          aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AB"),
          aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AC"),
        ),
        nomisIdsToAreaCodes,
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

    val results = service.getCvlRecords(
      listOf(
        aPrisonerSearchPrisoner,
        aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AB"),
        aPrisonerSearchPrisoner.copy(prisonerNumber = "A1234AC"),
      ),
      nomisIdsToAreaCodes,
    )
    assertThat(results).containsExactlyInAnyOrder(
      CvlCaseDto(
        nomisId = aPrisonerSearchPrisoner.prisonerNumber,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        isEligible = true,
        eligibleKind = LicenceKind.CRD,
        ineligiblityReasons = anEligibilityAssessment().ineligiblityReasons,
      ),
      CvlCaseDto(
        nomisId = "A1234AB",
        licenceStartDate = LocalDate.of(2022, 11, 23),
        isEligible = true,
        eligibleKind = LicenceKind.PRRD,
        ineligiblityReasons = prrdEligibilityAssessment.ineligiblityReasons,
      ),
      CvlCaseDto(
        nomisId = "A1234AC",
        licenceStartDate = null,
        isEligible = false,
        eligibleKind = null,
        ineligiblityReasons = ineligibleEligibilityAssessment.ineligiblityReasons,
      ),
    )
  }

  @Test
  fun `it builds the CvlCaseDto for an individual case`() {
    whenever(
      eligibilityService.getEligibilityAssessments(
        listOf(aPrisonerSearchPrisoner),
        mapOf(aPrisonerSearchPrisoner.prisonerNumber to "AREA1"),
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

    val result = service.getCvlRecord(aPrisonerSearchPrisoner, "AREA1")

    assertThat(result).isEqualTo(
      CvlCaseDto(
        nomisId = aPrisonerSearchPrisoner.prisonerNumber,
        licenceStartDate = LocalDate.of(2021, 10, 22),
        isEligible = true,
        eligibleKind = LicenceKind.CRD,
        ineligiblityReasons = anEligibilityAssessment().ineligiblityReasons,
      ),
    )
  }

  private val aPrisonerSearchPrisoner = prisonerSearchResult()
  private val nomisIdsToAreaCodes = mapOf(
    aPrisonerSearchPrisoner.prisonerNumber to "AREA1",
    "A1234AB" to "AREA2",
    "A1234AC" to "AREA3",
  )
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
