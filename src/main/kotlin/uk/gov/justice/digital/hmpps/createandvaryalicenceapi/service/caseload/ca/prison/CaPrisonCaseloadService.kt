package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCasesRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

@Service
class CaPrisonCaseloadService(
  private val licenceCasesRepository: LicenceCasesRepository,
  private val deliusApiClient: DeliusApiClient,
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
    val filteredPrisons = prisonCaseload.filterNot { it == "CADM" }

    val licenceCases = licenceCasesRepository.findLicenceCases(statuses, filteredPrisons)

    val eligibleExistingCases = existingCasesCaseloadService.filterAndFormatExistingCases(licenceCases)

    val eligibleNotStartedCases =
      notStartedCaseloadService.findAndFormatNotStartedCases(licenceCases, prisonCaseload)

    val cases = mapCasesToComs(eligibleExistingCases + eligibleNotStartedCases)

    val results = applySearch(searchString, cases)
    return results.sortedWith(compareBy<CaCase> { it.releaseDate }.thenBy { it.licenceId })
  }

  private fun mapCasesToComs(casesToMap: List<CaCase>): List<CaCase> {
    val cases = splitCasesByComDetails(casesToMap)

    val noComPrisonerNumbers = cases.withNoComId.map { c -> c.prisonerNumber }
    val coms = deliusApiClient.getOffenderManagers(noComPrisonerNumbers).associateBy { it.case.nomisId }

    // if no code or username, hit delius to find COM details
    val caCaseListWithNoComId = cases.withNoComId.map { caCase ->
      val com = coms[caCase.prisonerNumber]
      if (com != null && !com.unallocated) {
        caCase.copy(
          probationPractitioner = ProbationPractitioner(
            staffCode = com.code,
            name = com.name.fullName(),
          ),
        )
      } else {
        caCase
      }
    }

    // If COM username but no code, do a separate call to use the data in CVL if it exists. Should help highlight any desync between Delius and CVL
    val comUsernames = cases.withStaffUsername.map { c -> c.probationPractitioner?.staffUsername!! }
    val deliusStaffNames =
      deliusApiClient.getStaffDetailsByUsername(comUsernames).associateBy { it.username?.lowercase() }

    val caCaseListWithStaffUsername = cases.withStaffUsername.map { caCase ->
      val com = deliusStaffNames[caCase.probationPractitioner?.staffUsername?.lowercase()]
      if (com != null) {
        caCase.copy(
          probationPractitioner = ProbationPractitioner(
            staffCode = com.code,
            name = com.name.fullName(),
          ),
        )
      } else {
        caCase
      }
    }

    return caCaseListWithNoComId + caCaseListWithStaffUsername + cases.withStaffCode
  }

  fun splitCasesByComDetails(cases: List<CaCase>): GroupedByCom = cases.fold(
    GroupedByCom(
      withStaffCode = emptyList(),
      withStaffUsername = emptyList(),
      withNoComId = emptyList(),
    ),
  ) { acc, caCase ->
    if (caCase.probationPractitioner?.staffCode != null) {
      acc.withStaffCode += caCase
    } else if (caCase.probationPractitioner?.staffUsername != null) {
      acc.withStaffUsername += caCase
    } else {
      acc.withNoComId += caCase
    }
    return@fold acc
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

  data class GroupedByCom(
    var withStaffCode: List<CaCase>,
    var withStaffUsername: List<CaCase>,
    var withNoComId: List<CaCase>,
  )
}
