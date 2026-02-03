package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.Clock

@Service
class ExistingCasesCaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val clock: Clock,
  private val releaseDateService: ReleaseDateService,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
) {

  fun findExistingCases(licences: List<LicenceCaCase>): List<CaCase> {
    val preReleaseLicences = licences.filter { it.statusCode != ACTIVE }
    if (preReleaseLicences.isEmpty()) {
      return emptyList()
    }

    val licenceNomisIds = preReleaseLicences.mapNotNull { it.prisonNumber }
    val prisonersWithLicences = prisonerSearchApiClient.searchPrisonersByNomisIds(licenceNomisIds)
    val nomisEnrichedLicences = enrichWithNomisData(preReleaseLicences, prisonersWithLicences)
    return filterExistingLicencesForEligibility(nomisEnrichedLicences)
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> = licences.filter { l -> l.nomisLegalStatus != "DEAD" }

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

  private fun enrichWithNomisData(
    licenceCaCases: List<LicenceCaCase>,
    nomisRecords: List<PrisonerSearchPrisoner>,
  ): List<CaCase> {
    val usernameToProbationPractitioner = mapCasesToProbationPractitioner(licenceCaCases)
    return nomisRecords.map { nomisRecord ->
      val licencesForOffender = licenceCaCases.filter { l -> l.prisonNumber == nomisRecord.prisonerNumber }
      if (licencesForOffender.isEmpty()) return@map null
      val licence = LatestLicenceFinder.findLatestLicenceCases(licencesForOffender)
      val isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence?.licenceStartDate, licence?.kind)
      val releaseDate = licence?.licenceStartDate
      CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOfId,
        name = licence.let { "${it?.forename} ${it?.surname}" },
        prisonerNumber = licence?.prisonNumber!!,
        releaseDate = releaseDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceCase(licence),
        licenceStatus = licence.statusCode,
        nomisLegalStatus = nomisRecord.legalStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isInHardStopPeriod = isInHardStopPeriod,
        tabType = Tabs.determineCaViewCasesTab(
          releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
          releaseDate,
          licence,
          timeServedCase = licence.kind == LicenceKind.TIME_SERVED,
          clock,
        ),
        probationPractitioner = usernameToProbationPractitioner[licence.comUsername?.lowercase()]
          ?: ProbationPractitioner.UNALLOCATED,
        prisonCode = licence.prisonCode,
        prisonDescription = licence.prisonDescription,
      )
    }.filterNotNull()
  }
}
