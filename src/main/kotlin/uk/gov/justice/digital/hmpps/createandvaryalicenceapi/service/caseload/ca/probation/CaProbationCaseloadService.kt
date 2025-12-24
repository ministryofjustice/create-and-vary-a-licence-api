package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.probation

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.CaProbationCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.SearchFilter
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
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
  private val telemetryService: TelemetryService,
) {
  val statuses = listOf(
    ACTIVE,
    VARIATION_APPROVED,
    VARIATION_IN_PROGRESS,
    VARIATION_SUBMITTED,
  )

  fun getProbationOmuCaseload(prisonCaseload: Set<String>, searchString: String?): List<CaCase> {
    val licences = licenceCaseRepository.findLicenceCases(statuses, prisonCaseload.toList())

    val cases = formatReleasedLicences(licences)
    telemetryService.recordCaseloadLoad(CaProbationCaseload, prisonCaseload, cases)

    val searchResults = SearchFilter.apply(searchString, cases)
    return searchResults.sortedWith(compareByDescending<CaCase> { it.releaseDate }.thenBy { it.licenceId })
  }

  private fun formatReleasedLicences(licences: List<LicenceCaCase>): List<CaCase> {
    val groupedLicences = licences.groupBy { it.prisonNumber }
    val usernameToProbationPractitioner = mapCasesToProbationPractitioner(licences)
    return groupedLicences.map {
      val licence = findRelevantLicence(it)
      val isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence?.licenceStartDate)
      CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOfId,
        name = "${licence?.forename} ${licence?.surname}",
        prisonerNumber = licence?.prisonNumber!!,
        releaseDate = licence.licenceStartDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceCase(licence),
        licenceStatus = licence.statusCode,
        lastWorkedOnBy = licence.updatedByFullName,
        isInHardStopPeriod = isInHardStopPeriod,
        probationPractitioner = usernameToProbationPractitioner[licence.comUsername?.lowercase()] ?: ProbationPractitioner.UNALLOCATED,
        prisonCode = licence.prisonCode,
        prisonDescription = licence.prisonDescription,
      )
    }
  }

  private fun findRelevantLicence(entry: Map.Entry<String?, List<LicenceCaCase>>): LicenceCaCase? = if (entry.value.size > 1) {
    entry.value.find { l -> l.statusCode != ACTIVE }
  } else {
    entry.value[0]
  }

  private fun mapCasesToProbationPractitioner(licences: List<LicenceCaCase>): Map<String, ProbationPractitioner> {
    val comUsernames = licences.mapNotNull { it.comUsername }.distinct()
    return deliusApiClient.getStaffDetailsByUsername(comUsernames)
      .filter { it.username != null }
      .associateBy { it.username!!.lowercase() }
      .mapValues { entry ->
        ProbationPractitioner(
          staffCode = entry.value.code,
          name = entry.value.name.fullName(),
          allocated = true,
        )
      }
  }
}
