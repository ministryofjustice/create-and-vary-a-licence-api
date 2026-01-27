package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.LocalDate

private const val CASELOAD_WINDOW = 5L

@Service
class TimeServedCaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val clock: Clock,
) {
  fun getCases(prisonCode: String): TimeServedCaseload {
    val today = LocalDate.now(clock)

    val cases = prisonerSearchApiClient.searchPrisonersByReleaseDate(
      today,
      today.plusDays(CASELOAD_WINDOW),
      setOf(prisonCode),
    )

    val (timeServedCases, nonTimeServedCases) = cases.map {
      TimeServedCase(
        name = it.firstName + " " + it.lastName,
        prisonerNumber = it.prisonerNumber,
        releaseDate = it.releaseDate,
        nomisLegalStatus = it.legalStatus,
        prisonCode = it.prisonId,
        isTimeServedCaseByCrdsRule = it.isEligibleViaCRDSRule(today),
        isTimeServedCaseByNonCrdsRule = it.isEligibleViaNonCRDSRule(today),
        isTimeServedCaseByAllPrisonRule = it.isEligibleViaAllPrisonRule(today),
        isTimeServedCaseByIgnoreArdRule = it.isTimeServedCaseByIgnoringArdRule(),
        sentenceStartDate = it.sentenceStartDate,
        conditionalReleaseDate = it.conditionalReleaseDate,
        conditionalReleaseDateOverride = it.conditionalReleaseDateOverrideDate,
        confirmedReleaseDate = it.confirmedReleaseDate,
      )
    }.partition { it.isTimeServedCase }

    return TimeServedCaseload(identifiedCases = timeServedCases, otherCases = nonTimeServedCases)
  }

  // For CRDS prisons, the hypothesis is that we can rely on CRD
  fun PrisonerSearchPrisoner.isEligibleViaCRDSRule(today: LocalDate) = //
    sentenceStartDate == today &&
      confirmedReleaseDate == today &&
      conditionalReleaseDate == today

  // Prisons not using CRDS tend to calculate CRD incorrectly and correct with CRD override
  fun PrisonerSearchPrisoner.isEligibleViaNonCRDSRule(today: LocalDate) = //
    sentenceStartDate == today &&
      confirmedReleaseDate == today &&
      conditionalReleaseDate == today.minusDays(1) &&
      conditionalReleaseDateOverrideDate == today

  // So the following should calculate it for both CRDS and non-CRD cases
  fun PrisonerSearchPrisoner.isEligibleViaAllPrisonRule(today: LocalDate) = //
    (
      sentenceStartDate == today &&
        confirmedReleaseDate == today &&
        (conditionalReleaseDateOverrideDate ?: conditionalReleaseDate) == today
      )

  // Prisons may not release on CRD but the next working day so we should ignore ARD and include cases with CRD in the past
  fun PrisonerSearchPrisoner.isTimeServedCaseByIgnoringArdRule() = //
    (
      sentenceStartDate == (conditionalReleaseDateOverrideDate ?: conditionalReleaseDate)
      )
}
