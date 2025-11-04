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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations
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

  private fun buildCreateCaseload(managedOffenders: List<ManagedOffenderCrn>): List<ComCase> {
    val deliusAndNomisRecords = pairDeliusRecordsWithNomis(managedOffenders)
    val nomisIdsToAreaCodes = deliusAndNomisRecords.map { (deliusRecord, nomisRecord) ->
      nomisRecord.prisonerNumber to (deliusRecord.team?.provider?.code ?: "")
    }.toMap()
    val cvlRecords =
      cvlRecordService.getCvlRecords(deliusAndNomisRecords.map { (_, nomisRecord) -> nomisRecord }, nomisIdsToAreaCodes)
    val eligibleCases = filterCasesEligibleForCvl(deliusAndNomisRecords, cvlRecords)
    val crnsToLicenceLists = mapCasesToLicences(eligibleCases, cvlRecords)
    val preReleaseCases = removeCasesWithActiveLicences(eligibleCases, crnsToLicenceLists)
    val crnsToDisplayLicences = findRelevantLicencePerCase(preReleaseCases, crnsToLicenceLists)
    val filteredCases = filterHdcAndFutureReleases(preReleaseCases, crnsToDisplayLicences)
    val crnsToComs = getResponsibleComs(filteredCases.map { (deliusRecord, _) -> deliusRecord }, crnsToDisplayLicences)
    return transformToCreateCaseload(filteredCases, crnsToDisplayLicences, crnsToComs)
  }

  fun mapCasesToLicences(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    cvlRecords: List<CvlRecord>,
  ): Map<String, List<CaseLoadLicenceSummary>> {
    val crns = cases.map { (deliusRecord, _) -> deliusRecord.crn!! }
    val licences = getExistingActiveAndPreReleaseLicences(crns)
    return cases.map { (deliusRecord, nomisRecord) ->
      val crn = deliusRecord.crn!!
      val offenderLicences = licences.filter { licence -> crn == licence.crn }
      val cvlRecord = cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }
      if (offenderLicences.isEmpty()) {
        return@map crn to listOf(createNotStartedLicence(deliusRecord, nomisRecord, cvlRecord))
      }
      return@map crn to offenderLicences
    }.toMap()
  }

  private fun pairDeliusRecordsWithNomis(managedOffenders: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> {
    val caseloadNomisIds = managedOffenders.mapNotNull { offender -> offender.nomisId }
    if (caseloadNomisIds.isEmpty()) {
      return emptyMap()
    }
    val nomisRecords = prisonerSearchApiClient.searchPrisonersByNomisIds(caseloadNomisIds)

    return managedOffenders.mapNotNull { deliusRecord ->
      val nomisRecord = nomisRecords.find { prisoner -> prisoner.prisonerNumber == deliusRecord.nomisId }
      if (nomisRecord != null) {
        deliusRecord to nomisRecord
      } else {
        null
      }
    }.toMap()
  }

  private fun filterCasesEligibleForCvl(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    cvlRecords: List<CvlRecord>,
  ): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> = cases.filter { (_, nomisRecord) ->
    val cvlRecord = cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }
    return@filter cvlRecord.isEligible
  }

  private fun removeCasesWithActiveLicences(
    eligibleCases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    crnsToLicences: Map<String, List<CaseLoadLicenceSummary>>,
  ): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> = eligibleCases.filter { (deliusRecord, _) ->
    val licences = crnsToLicences[deliusRecord.crn]!!
    licences.none { it.licenceStatus == ACTIVE }
  }

  private fun findRelevantLicencePerCase(
    eligibleCases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    crnsToLicences: Map<String, List<CaseLoadLicenceSummary>>,
  ): Map<String, CaseLoadLicenceSummary> = eligibleCases.map { (deliusRecord, _) ->
    val licences = crnsToLicences[deliusRecord.crn]!!
    deliusRecord.crn!! to RelevantLicenceFinder.findRelevantLicencePerCase(licences)
  }.toMap()

  private fun createNotStartedLicence(
    deliusRecord: ManagedOffenderCrn,
    nomisRecord: PrisonerSearchPrisoner,
    cvlRecord: CvlRecord,
  ): CaseLoadLicenceSummary {
    val licenceType = LicenceType.getLicenceType(nomisRecord)
    val licenceKind = cvlRecord.eligibleKind!!
    val name = "${nomisRecord.firstName} ${nomisRecord.lastName}".trim().convertToTitleCase()

    var licenceStatus = NOT_STARTED
    if (cvlRecord.isInHardStopPeriod) {
      licenceStatus = TIMED_OUT
    }
    return CaseLoadLicenceSummary(
      licenceStatus = licenceStatus,
      licenceType = licenceType,
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
  }

  @TimeServedConsiderations("If the COM is an unallocated member of the team, do we need to handle anything differently here?")
  private fun getResponsibleComs(
    caseload: List<ManagedOffenderCrn>,
    crnsToDisplayLicences: Map<String, CaseLoadLicenceSummary>,
  ): Map<String, ProbationPractitioner?> {
    val comUsernames = crnsToDisplayLicences.mapNotNull { (_, licence) -> licence.comUsername }.distinct()

    val coms = deliusApiClient.getStaffDetailsByUsername(comUsernames).associateBy { it.username?.lowercase() }
    return caseload.associate { case ->
      val licence = crnsToDisplayLicences[case.crn]
      val responsibleCom = coms[licence?.comUsername?.lowercase()]
      if (responsibleCom != null) {
        case.crn!! to ProbationPractitioner(
          responsibleCom.code,
          name = responsibleCom.name.fullName(),
        )
      } else {
        if (case.staff == null || case.staff.unallocated == true) {
          case.crn!! to null
        } else {
          case.crn!! to ProbationPractitioner(
            staffCode = case.staff.code,
            name = case.staff.name?.fullName(),
          )
        }
      }
    }
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

  private fun filterHdcAndFutureReleases(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    crnsToLicences: Map<String, CaseLoadLicenceSummary>,
  ): Map<ManagedOffenderCrn, PrisonerSearchPrisoner> {
    val hdcStatuses =
      hdcService.getHdcStatus(cases.map { (_, nomisRecord) -> nomisRecord })
    return cases.filter { (deliusRecord, nomisRecord) ->
      val kind = crnsToLicences[deliusRecord.crn]!!.kind
      val bookingId = nomisRecord.bookingId?.toLong()!!
      hdcStatuses.canBeSeenByCom(kind, bookingId)
    }.filter { (deliusRecord, _) ->
      crnsToLicences[deliusRecord.crn]!!.releaseDate.isTodayOrInTheFuture()
    }
  }

  private fun getTeamCode(probationTeamCodes: List<String>, teamSelected: List<String>): String = if (teamSelected.isNotEmpty()) {
    teamSelected.first()
  } else {
    probationTeamCodes.first()
  }

  private fun transformToCreateCaseload(
    cases: Map<ManagedOffenderCrn, PrisonerSearchPrisoner>,
    crnsToLicences: Map<String, CaseLoadLicenceSummary>,
    crnsToComs: Map<String, ProbationPractitioner?>,
  ): List<ComCase> = cases.map { (deliusRecord) ->
    val licence = crnsToLicences[deliusRecord.crn]!!
    val probationPractitioner = crnsToComs[deliusRecord.crn]
    ComCase(
      licenceId = licence.licenceId,
      licenceStatus = licence.licenceStatus,
      licenceType = licence.licenceType,
      name = licence.name,
      crnNumber = licence.crn,
      prisonerNumber = licence.nomisId,
      releaseDate = licence.releaseDate,
      probationPractitioner = probationPractitioner,
      hardStopDate = licence.hardStopDate,
      hardStopKind = licence.hardStopKind,
      hardStopWarningDate = licence.hardStopWarningDate,
      kind = licence.kind,
      licenceCreationType = licence.licenceCreationType,
      isReviewNeeded = licence.isReviewNeeded,
    )
  }.sortedWith(compareBy<ComCase> { it.releaseDate }.thenBy { it.name })

  private fun getExistingActiveAndPreReleaseLicences(crnList: List<String>): List<CaseLoadLicenceSummary> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceCaseRepository.findLicenceCasesForCom(
      crns = crnList,
      statusCodes = listOf(
        ACTIVE,
        IN_PROGRESS,
        SUBMITTED,
        APPROVED,
        TIMED_OUT,
      ),
    ).map { mapToCaseLoadLicenceSummary(it) }
  }
}
