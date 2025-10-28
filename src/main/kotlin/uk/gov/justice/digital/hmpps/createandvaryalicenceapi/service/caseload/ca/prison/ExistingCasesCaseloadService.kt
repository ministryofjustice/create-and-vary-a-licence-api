package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
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

  fun findExistingCases(licences: List<LicenceSummary>): List<CaCase> {
    val preReleaseLicences = licences.filter { it.licenceStatus != ACTIVE }
    if (preReleaseLicences.isEmpty()) {
      return emptyList()
    }

    val licenceNomisIds = preReleaseLicences.map { it.nomisId }
    val prisonersWithLicences = prisonerSearchApiClient.searchPrisonersByNomisIds(licenceNomisIds)
    val nomisEnrichedLicences = enrichWithNomisData(preReleaseLicences, prisonersWithLicences)
    return filterExistingLicencesForEligibility(nomisEnrichedLicences)
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> = licences.filter { l -> l.nomisLegalStatus != "DEAD" }

  private fun mapCasesToProbationPractitioner(licences: List<LicenceSummary>): Map<String, ProbationPractitioner> {
    val comUsernames = licences.mapNotNull { it.comUsername }.distinct()
    return deliusApiClient.getStaffDetailsByUsername(comUsernames)
      .filter { it.username != null }
      .associateBy { it.username!!.lowercase() }
      .mapValues { entry ->
        ProbationPractitioner(
          staffCode = entry.value.code,
          name = entry.value.name.fullName(),
        )
      }
  }

  private fun enrichWithNomisData(
    licences: List<LicenceSummary>,
    nomisRecords: List<PrisonerSearchPrisoner>,
  ): List<CaCase> {
    val usernameToProbationPractitioner = mapCasesToProbationPractitioner(licences)
    return nomisRecords.map { nomisRecord ->
      val licencesForOffender = licences.filter { l -> l.nomisId == nomisRecord.prisonerNumber }
      if (licencesForOffender.isEmpty()) return@map null
      val licence = LatestLicenceFinder.findLatestLicenceSummary(licencesForOffender)
      val releaseDate = licence?.licenceStartDate
      CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOf,
        name = licence.let { "${it?.forename} ${it?.surname}" },
        prisonerNumber = licence?.nomisId!!,
        releaseDate = releaseDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceSummary(licence),
        licenceStatus = licence.licenceStatus,
        nomisLegalStatus = nomisRecord.legalStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isInHardStopPeriod = licence.isInHardStopPeriod,
        tabType = Tabs.determineCaViewCasesTab(
          releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
          releaseDate,
          licence,
          clock,
        ),
        probationPractitioner = usernameToProbationPractitioner[licence.comUsername],
        prisonCode = licence.prisonCode,
        prisonDescription = licence.prisonDescription,
      )
    }.filterNotNull()
  }
}
