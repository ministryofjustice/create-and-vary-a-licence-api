package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.probation

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED

@Service
class CaProbationCaseloadService(
  private val licenceCaseRepository: LicenceCaseRepository,
  private val releaseDateService: ReleaseDateService,
  private val deliusApiClient: DeliusApiClient,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
) {
  val statuses = listOf(
    ACTIVE,
    VARIATION_APPROVED,
    VARIATION_IN_PROGRESS,
    VARIATION_SUBMITTED,
  )

  fun getProbationOmuCaseload(prisonCaseload: Set<String>, searchString: String?): List<CaCase> {
    val licenceCases = licenceCaseRepository.findLicenceCases(statuses, prisonCaseload.toList())

    val formattedLicences = formatReleasedLicences(licenceCases)
    val cases = mapCasesToComs(formattedLicences)

    val searchResults = applySearch(searchString, cases)
    return searchResults.sortedWith(compareByDescending<CaCase> { it.releaseDate }.thenBy { it.licenceId })
  }

  private fun formatReleasedLicences(licenceCaCases: List<LicenceCaCase>): List<CaCase> {
    val groupedLicences = licenceCaCases.groupBy { it.prisonNumber }
    return groupedLicences.map {
      val licenceCase = if (it.value.size > 1) {
        it.value.find { l -> l.licenceStatus != ACTIVE }
      } else {
        it.value[0]
      }

      val isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceCase?.licenceStartDate)
      CaCase(
        kind = licenceCase?.kind,
        licenceId = licenceCase?.licenceId,
        licenceVersionOf = licenceCase?.versionOfId,
        name = "${licenceCase?.forename} ${licenceCase?.surname}",
        prisonerNumber = licenceCase?.prisonNumber!!,
        releaseDate = licenceCase.licenceStartDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceCase(licenceCase),
        licenceStatus = licenceCase.licenceStatus,
        lastWorkedOnBy = licenceCase.updatedByFullName,
        isInHardStopPeriod = isInHardStopPeriod,
        probationPractitioner = ProbationPractitioner(
          staffUsername = licenceCase.comUsername,
        ),
        prisonCode = licenceCase.prisonCode,
        prisonDescription = licenceCase.prisonDescription,
      )
    }
  }

  private fun mapCasesToComs(cases: List<CaCase>): List<CaCase> {
    val comUsernames = cases.map { c -> c.probationPractitioner?.staffUsername!! }
    val deliusStaffNames =
      deliusApiClient.getStaffDetailsByUsername(comUsernames).associateBy { it.username?.lowercase() }

    return cases.map { caCase ->
      val com = deliusStaffNames[caCase.probationPractitioner?.staffUsername?.lowercase()]
      if (com != null) {
        caCase.copy(
          probationPractitioner = toProbationPractioner(com),
        )
      } else {
        caCase
      }
    }
  }

  private fun toProbationPractioner(com: StaffNameResponse) = ProbationPractitioner(
    staffCode = com.code,
    name = com.name.fullName(),
  )

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
