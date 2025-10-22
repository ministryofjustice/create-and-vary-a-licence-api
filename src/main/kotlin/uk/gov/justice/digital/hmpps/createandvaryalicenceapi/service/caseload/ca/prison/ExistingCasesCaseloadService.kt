package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.Clock
import java.time.LocalDate

@Service
class ExistingCasesCaseloadService(
  private val caseloadService: CaseloadService,
  private val clock: Clock,
  private val releaseDateService: ReleaseDateService,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
) {

  fun filterAndFormatExistingCases(licenceCaCases: List<LicenceCaCase>): List<CaCase> {
    val preReleaseLicenceCases = licenceCaCases.filter { it.licenceStatus != ACTIVE }
    if (preReleaseLicenceCases.isEmpty()) {
      return emptyList()
    }

    val licenceNomisIds = preReleaseLicenceCases.map { it.prisonNumber }
    val prisonersWithLicences = caseloadService.getPrisonersByNumber(licenceNomisIds)
    val nomisEnrichedLicences = enrichWithNomisData(preReleaseLicenceCases, prisonersWithLicences)
    return filterExistingLicencesForEligibility(nomisEnrichedLicences)
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> = licences.filter { l -> l.nomisLegalStatus != "DEAD" }

  private fun enrichWithNomisData(
    licenceCaCases: List<LicenceCaCase>,
    nomisRecords: List<PrisonerSearchPrisoner>,
  ): List<CaCase> {
    return nomisRecords.map { nomisRecord ->
      val licencesForOffender = licenceCaCases.filter { l -> l.prisonNumber == nomisRecord.prisonerNumber }
      if (licencesForOffender.isEmpty()) return@map null
      val licenceCase = LatestLicenceFinder.findLatestLicenceCases(licencesForOffender)
      val releaseDate = licenceCase?.licenceStartDate
      val isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceCase?.licenceStartDate)

      val probationPractitioner = ProbationPractitioner(
        staffUsername = licenceCase?.comUsername,
      )

      CaCase(
        kind = licenceCase?.kind,
        licenceId = licenceCase?.licenceId,
        licenceVersionOf = licenceCase?.versionOfId,
        name = licenceCase?.fullName ?: "",
        prisonerNumber = licenceCase?.prisonNumber!!,
        releaseDate = releaseDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceCase(licenceCase),
        licenceStatus = licenceCase.licenceStatus,
        nomisLegalStatus = nomisRecord.legalStatus,
        lastWorkedOnBy = licenceCase.updatedByFullName,
        isInHardStopPeriod = isInHardStopPeriod,
        tabType = createTabType(licenceCase, releaseDate),
        probationPractitioner = probationPractitioner,
        prisonCode = licenceCase.prisonCode,
        prisonDescription = licenceCase.prisonDescription,
      )
    }.filterNotNull()
  }

  private fun createTabType(
    licenceCaCase: LicenceCaCase,
    releaseDate: LocalDate?,
  ): CaViewCasesTab = Tabs.determineCaViewCasesTab(
    releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licenceCaCase),
    releaseDate,
    licenceCaCase,
    clock,
  )
}
