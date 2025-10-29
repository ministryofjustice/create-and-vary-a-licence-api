package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

private const val CENTRAL_ADMIN_CASELOAD = "CADM"

@Service
class CaPrisonCaseloadService(
  private val licenceCaseRepository: LicenceCaseRepository,
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
    val licenceCases = licenceCaseRepository.findLicenceCases(statuses, filteredPrisons)

    val existingCases = existingCasesCaseloadService.findExistingCases(licenceCases)
    val notStartedCases = notStartedCaseloadService.findNotStartedCases(licenceCases, prisonCaseload)

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
