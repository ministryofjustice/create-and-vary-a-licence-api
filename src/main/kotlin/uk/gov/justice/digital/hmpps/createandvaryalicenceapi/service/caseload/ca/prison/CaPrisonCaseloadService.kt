package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

private const val CENTRAL_ADMIN_CASELOAD = "CADM"

@Service
class CaPrisonCaseloadService(
  private val licenceService: LicenceService,
  private val existingCasesCaseloadService: ExistingCasesCaseloadService,
  private val notStartedCaseloadService: NotStartedCaseloadService,
) {
  val statuses = listOf(
    APPROVED,
    SUBMITTED,
    IN_PROGRESS,
    TIMED_OUT,
    ACTIVE,
  )

  fun getPrisonOmuCaseload(prisonCaseload: Set<String>, searchString: String?): List<CaCase> {
    val filteredPrisons = prisonCaseload.filterNot { it == CENTRAL_ADMIN_CASELOAD }
    val existingLicences = licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(statusCodes = statuses, prisonCodes = filteredPrisons, sortBy = "licenceStartDate"),
    )

    val existingCases = existingCasesCaseloadService.findExistingCases(existingLicences)
    val notStartedCases = notStartedCaseloadService.findNotStartedCases(existingLicences, prisonCaseload)

    val cases = existingCases + notStartedCases
    val results = applySearch(searchString, cases)
    return results.sortedWith(compareBy<CaCase> { it.releaseDate }.thenBy { it.licenceId })
  }

  private fun applySearch(searchString: String?, cases: List<CaCase>): List<CaCase> {
    if (searchString == null) {
      return cases
    }
    val term = searchString.lowercase()
    return cases.filter {
      it.name.lowercase().contains(term) ||
        it.prisonerNumber.lowercase().contains(term) ||
        it.probationPractitioner?.name?.lowercase()?.contains(term) ?: false
    }
  }
}
