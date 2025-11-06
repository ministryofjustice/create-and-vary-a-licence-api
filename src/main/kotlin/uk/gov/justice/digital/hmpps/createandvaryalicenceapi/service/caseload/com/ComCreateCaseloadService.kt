package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComCreateStaffCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadType.ComCreateTeamCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com.RelevantLicenceFinder.findRelevantLicencePerCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isTodayOrInTheFuture

@Service
class ComCreateCaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val licenceCaseRepository: LicenceCaseRepository,
  private val hdcService: HdcService,
  private val releaseDateService: ReleaseDateService,
  private val cvlRecordService: CvlRecordService,
  private val telemetryService: TelemetryService,
) {
  companion object {
    private val COM_CREATE_LICENCE_STATUSES = listOf(ACTIVE, IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT)
  }

  fun getStaffCreateCaseload(deliusStaffIdentifier: Long): List<ComCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val cases = buildCreateCaseload(managedOffenders)

    telemetryService.recordCaseloadLoad(ComCreateStaffCaseload, setOf(deliusStaffIdentifier.toString()), cases)
    return cases
  }

  fun getTeamCreateCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val cases = buildCreateCaseload(managedOffenders)

    telemetryService.recordCaseloadLoad(ComCreateTeamCaseload, setOf(teamCode), cases)
    return cases
  }

  private fun getTeamCode(probationTeamCodes: List<String>, teamSelected: List<String>): String = if (teamSelected.isNotEmpty()) {
    teamSelected.first()
  } else {
    probationTeamCodes.first()
  }

  private fun buildCreateCaseload(managedOffenders: List<ManagedOffenderCrn>): List<ComCase> {
    val deliusAndNomisRecords = pairDeliusRecordsWithNomis(managedOffenders)
    val nomisIdsToAreaCodes = deliusAndNomisRecords.map { (deliusRecord, nomisRecord) ->
      nomisRecord.prisonerNumber to (deliusRecord.team?.provider?.code ?: "")
    }.toMap()
    val cvlRecords =
      cvlRecordService.getCvlRecords(deliusAndNomisRecords.map { (_, nomisRecord) -> nomisRecord }, nomisIdsToAreaCodes)
    val eligibleCases = filterCasesEligibleForCvl(deliusAndNomisRecords, cvlRecords)
    val cases = getCasesWithLicences(eligibleCases, cvlRecords)
    val filteredCases = filterHdcAndFutureReleases(cases)

    return transformToCreateCaseload(filteredCases)
  }

  private fun pairDeliusRecordsWithNomis(managedOffenders: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> {
    val caseloadNomisIds = managedOffenders.mapNotNull { offender -> offender.nomisId }

    val nomisRecords =
      prisonerSearchApiClient.searchPrisonersByNomisIds(caseloadNomisIds).associateBy { it.prisonerNumber }

    return managedOffenders.mapNotNull { deliusRecord -> nomisRecords[deliusRecord.nomisId]?.let { deliusRecord to it } }
      .toMap()
  }

  private fun filterCasesEligibleForCvl(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    cvlRecords: List<CvlRecord>,
  ): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> = cases.filter { (_, nomisRecord) ->
    val cvlRecord = cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }
    return@filter cvlRecord.isEligible
  }

  private fun getCasesWithLicences(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    cvlRecords: List<CvlRecord>,
  ): List<Case> {
    val crns = cases.map { (deliusRecord, _) -> deliusRecord.crn!! }
    val licencesByCrn = getExistingActiveAndPreReleaseLicences(crns).groupBy { it.crn }
    val cvlRecordsByNomisId = cvlRecords.associateBy { it.nomisId }

    return cases.mapNotNull { (deliusRecord, nomisRecord) ->
      val licences = licencesByCrn[deliusRecord.crn!!] ?: emptyList()
      val cvlRecord = cvlRecordsByNomisId[nomisRecord.prisonerNumber]!!
      val probationPractitioner = deliusRecord.getProbationPractitioner()

      when {
        // No licences found for this offender so treat as a not started case
        licences.isEmpty() -> Case(
          probationPractitioner,
          nomisRecord,
          cvlRecord,
          createNotStartedLicence(deliusRecord, nomisRecord, cvlRecord),
        )

        // Has an active licence so shouldn't appear in create caseload
        licences.any { it.licenceStatus == ACTIVE } ->
          null

        // Should appear in create caseload with relevant licence
        else ->
          Case(probationPractitioner, nomisRecord, cvlRecord, findRelevantLicencePerCase(licences))
      }
    }
  }

  private fun getExistingActiveAndPreReleaseLicences(crnList: List<String>): List<CaseLoadLicenceSummary> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceCaseRepository.findLicenceCasesForCom(crns = crnList, statusCodes = COM_CREATE_LICENCE_STATUSES)
      .map { mapToCaseLoadLicenceSummary(it) }
  }

  private fun mapToCaseLoadLicenceSummary(licenceSummary: LicenceComCase): CaseLoadLicenceSummary {
    val hardStopDate = releaseDateService.getHardStopDate(licenceSummary.licenceStartDate)
    val hardStopWarningDate = releaseDateService.getHardStopWarningDate(licenceSummary.licenceStartDate)
    val isDueToBeReleasedInTheNextTwoWorkingDays =
      releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licenceSummary.licenceStartDate)

    return CaseLoadLicenceSummary(
      licenceId = licenceSummary.licenceId,
      licenceStatus = licenceSummary.statusCode,
      kind = licenceSummary.kind,
      crn = licenceSummary.crn,
      nomisId = licenceSummary.prisonNumber!!,
      name = licenceSummary.fullName,
      licenceType = licenceSummary.typeCode,
      comUsername = licenceSummary.comUsername,
      dateCreated = licenceSummary.dateCreated,
      approvedBy = licenceSummary.approvedByName,
      approvedDate = licenceSummary.approvedDate,
      versionOf = licenceSummary.versionOfId,
      updatedByFullName = licenceSummary.updatedByFullName,
      hardStopWarningDate = hardStopWarningDate,
      hardStopDate = hardStopDate,
      licenceStartDate = licenceSummary.licenceStartDate,
      releaseDate = licenceSummary.licenceStartDate,
      isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
      isReviewNeeded = licenceSummary.isReviewNeeded(),
    )
  }

  private fun ManagedOffenderCrn?.getProbationPractitioner() = this?.staff
    ?.takeUnless { it.unallocated == true }
    ?.let {
      ProbationPractitioner(
        staffCode = it.code,
        name = it.name?.fullName(),
      )
    }

  private fun createNotStartedLicence(
    deliusRecord: ManagedOffenderCrn,
    nomisRecord: PrisonerSearchPrisoner,
    cvlRecord: CvlRecord,
  ): CaseLoadLicenceSummary {
    val licenceKind = cvlRecord.eligibleKind!!
    val name = "${nomisRecord.firstName} ${nomisRecord.lastName}".trim().convertToTitleCase()

    val licenceStatus = if (cvlRecord.isInHardStopPeriod) {
      TIMED_OUT
    } else {
      NOT_STARTED
    }

    val caseLoadSummary = CaseLoadLicenceSummary(
      licenceStatus = licenceStatus,
      licenceType = cvlRecord.licenceType,
      crn = deliusRecord.crn,
      nomisId = nomisRecord.prisonerNumber,
      name = name,
      releaseDate = cvlRecord.licenceStartDate,
      kind = licenceKind,
      hardStopKind = cvlRecord.hardStopKind,
      hardStopDate = cvlRecord.hardStopDate,
      hardStopWarningDate = cvlRecord.hardStopWarningDate,
      isDueToBeReleasedInTheNextTwoWorkingDays = cvlRecord.isDueToBeReleasedInTheNextTwoWorkingDays,
    )
    return findRelevantLicencePerCase(listOf(caseLoadSummary))
  }

  private fun filterHdcAndFutureReleases(
    cases: List<Case>,
  ): List<Case> {
    val hdcStatuses = hdcService.getHdcStatus(cases.map { it.nomisRecord })
    return cases.filter {
      val kind = it.licenceSummary.kind
      val bookingId = it.nomisRecord.bookingId?.toLong()!!
      hdcStatuses.canBeSeenByCom(kind, bookingId) && it.licenceSummary.releaseDate.isTodayOrInTheFuture()
    }
  }

  private fun transformToCreateCaseload(cases: List<Case>): List<ComCase> = cases.map {
    with(it.licenceSummary) {
      ComCase(
        licenceId = licenceId,
        licenceStatus = licenceStatus,
        licenceType = licenceType,
        name = name,
        crnNumber = crn,
        prisonerNumber = nomisId,
        releaseDate = releaseDate,
        probationPractitioner = it.probationPractitioner,
        hardStopDate = hardStopDate,
        hardStopKind = hardStopKind,
        hardStopWarningDate = hardStopWarningDate,
        kind = kind,
        licenceCreationType = licenceCreationType,
        isReviewNeeded = isReviewNeeded,
      )
    }
  }.sortedWith(compareBy<ComCase> { it.releaseDate }.thenBy { it.name })

  private data class Case(
    val probationPractitioner: ProbationPractitioner?,
    val nomisRecord: PrisonerSearchPrisoner,
    val cvlRecord: CvlRecord,
    val licenceSummary: CaseLoadLicenceSummary,
  )
}
