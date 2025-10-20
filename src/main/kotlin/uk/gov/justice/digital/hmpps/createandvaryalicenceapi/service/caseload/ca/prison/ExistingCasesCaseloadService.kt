package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.Clock

@Service
class ExistingCasesCaseloadService(
  private val caseloadService: CaseloadService,
  private val clock: Clock,
  private val releaseDateService: ReleaseDateService,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
) {

  fun filterAndFormatExistingCases(licenceCases: List<LicenceCase>): List<CaCase> {
    val preReleaseLicenceCases = licenceCases.filter { it.licenceStatus != ACTIVE }
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
    licenceCases: List<LicenceCase>,
    nomisRecords: List<PrisonerSearchPrisoner>,
  ): List<CaCase> {
    return nomisRecords.map { nomisRecord ->
      val licencesForOffender = licenceCases.filter { l -> l.prisonNumber == nomisRecord.prisonerNumber }
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
        name = licenceCase.let { "${it?.forename} ${it?.surname}" },
        prisonerNumber = licenceCase?.prisonNumber!!,
        releaseDate = releaseDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceCase(licenceCase),
        licenceStatus = licenceCase.licenceStatus,
        nomisLegalStatus = nomisRecord.legalStatus,
        lastWorkedOnBy = licenceCase.updatedByFullName,
        isInHardStopPeriod = isInHardStopPeriod,
        tabType = Tabs.determineCaViewCasesTab(
          isDueToBeReleased(licenceCase),
          releaseDate,
          licenceCase,
          clock,
        ),
        probationPractitioner = probationPractitioner,
        prisonCode = licenceCase.prisonCode,
        prisonDescription = licenceCase.prisonDescription,
      )
    }.filterNotNull()
  }

  private fun isDueToBeReleased(licenceCase: LicenceCase?) =
    releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
      createSentenceDateHolder(licenceCase),
    )

  private fun createSentenceDateHolder(licenceCase: LicenceCase?): SentenceDateHolder = object : SentenceDateHolder {
    override val conditionalReleaseDate = licenceCase?.conditionalReleaseDate
    override val actualReleaseDate = licenceCase?.actualReleaseDate
    override val licenceStartDate = licenceCase?.licenceStartDate
    override val homeDetentionCurfewActualDate = licenceCase?.homeDetentionCurfewActualDate
    override val postRecallReleaseDate = licenceCase?.postRecallReleaseDate
  }
}
