package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toPrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.determineReleaseDateKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isTodayOrInTheFuture

@Service
class ComCreateCaseloadService(
  private val caseloadService: CaseloadService,
  private val deliusApiClient: DeliusApiClient,
  private val licenceService: LicenceService,
  private val eligibilityService: EligibilityService,
  private val hdcService: HdcService,
  private val licenceCreationService: LicenceCreationService,
  private val releaseDateService: ReleaseDateService,
) {

  fun getStaffCreateCaseload(deliusStaffIdentifier: Long): List<ComCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    return buildCreateCaseload(managedOffenders)
  }

  fun getTeamCreateCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    return buildCreateCaseload(managedOffenders)
  }

  private fun buildCreateCaseload(managedOffenders: List<ManagedOffenderCrn>): List<ComCase> {
    val deliusAndNomisRecords = pairDeliusRecordsWithNomis(managedOffenders)
    val crnsToLicenceLists = mapCasesToLicences(deliusAndNomisRecords)
    val eligibleCases = filterCasesEligibleForCvl(deliusAndNomisRecords, crnsToLicenceLists)
    val crnsToDisplayLicences = findRelevantLicencePerCase(eligibleCases, crnsToLicenceLists)
    val filteredCases = filterHdcAndFutureReleases(eligibleCases, crnsToDisplayLicences)
    val crnsToComs = getResponsibleComs(filteredCases.map { (deliusRecord, _) -> deliusRecord }, crnsToDisplayLicences)
    return transformToCreateCaseload(filteredCases, crnsToDisplayLicences, crnsToComs)
  }

  fun mapCasesToLicences(cases: Map<ManagedOffenderCrn, CaseloadItem>): Map<String, List<CaseLoadLicenceSummary>> {
    val crns = cases.map { (deliusRecord, _) -> deliusRecord.crn!! }
    val licences = findExistingActiveAndPreReleaseLicences(crns)
    return cases.map { (deliusRecord, nomisRecord) ->
      val crn = deliusRecord.crn!!
      val offenderLicences = licences.filter { licence -> crn == licence.crn }
      if (offenderLicences.isEmpty()) {
        return@map crn to listOf(createNotStartedLicence(deliusRecord, nomisRecord))
      }
      return@map crn to offenderLicences
    }.toMap()
  }

  private fun pairDeliusRecordsWithNomis(managedOffenders: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, CaseloadItem> {
    val caseloadNomisIds = managedOffenders.mapNotNull { offender -> offender.nomisId }
    if (caseloadNomisIds.isEmpty()) {
      return emptyMap()
    }
    val nomisRecords = caseloadService.getPrisonersByNumber(caseloadNomisIds)

    return managedOffenders.mapNotNull { deliusRecord ->
      val nomisRecord = nomisRecords.find { prisoner -> prisoner.prisoner.prisonerNumber == deliusRecord.nomisId }
      if (nomisRecord != null) {
        deliusRecord to nomisRecord
      } else {
        null
      }
    }.toMap()
  }

  private fun filterCasesEligibleForCvl(
    cases: Map<ManagedOffenderCrn, CaseloadItem>,
    crnsToLicences: Map<String, List<CaseLoadLicenceSummary>>,
  ): Map<ManagedOffenderCrn, CaseloadItem> = cases.filter { (deliusRecord, nomisRecord) ->
    val prisoner = nomisRecord.prisoner.toPrisonerSearchPrisoner()
    val licences = crnsToLicences[deliusRecord.crn]!!
    val prrdLicence = licences.find {
      determineReleaseDateKind(
        prisoner.postRecallReleaseDate,
        prisoner.conditionalReleaseDate,
      ) == LicenceKind.PRRD
    }
    when {
      prisoner.bookingId == null -> false
      licences.any { it.licenceStatus == ACTIVE } -> false
      prrdLicence != null && prrdLicence.licenceId != null -> true
      !eligibilityService.isEligibleForCvl(prisoner, deliusRecord.team?.provider?.code) -> false
      else -> true
    }
  }

  private fun findRelevantLicencePerCase(
    eligibleCases: Map<ManagedOffenderCrn, CaseloadItem>,
    crnsToLicences: Map<String, List<CaseLoadLicenceSummary>>,
  ): Map<String, CaseLoadLicenceSummary> = eligibleCases.map { (deliusRecord, _) ->
    val licences = crnsToLicences[deliusRecord.crn]!!
    val licenceToDisplay = when {
      licences.any { it.kind == LicenceKind.HARD_STOP } -> findHardStopLicenceToDisplay(licences)
      licences.any { it.licenceStatus == TIMED_OUT } -> findTimedOutLicenceToDisplay(licences)
      else -> findLicenceToDisplay(licences)
    }
    deliusRecord.crn!! to licenceToDisplay
  }.toMap()

  private fun createNotStartedLicence(
    deliusRecord: ManagedOffenderCrn,
    nomisRecord: CaseloadItem,
  ): CaseLoadLicenceSummary {
    val prisoner = nomisRecord.prisoner.toPrisonerSearchPrisoner()
    val licenceType = LicenceType.getLicenceType(prisoner)
    val licenceKind = licenceCreationService.determineLicenceKind(prisoner, nomisRecord.licenceStartDate)
    val name = "${prisoner.firstName} ${prisoner.lastName}".trim().convertToTitleCase()
    val sentenceDateHolder = prisoner.toSentenceDateHolder(nomisRecord.licenceStartDate)

    var licenceStatus = NOT_STARTED
    if (releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate)) {
      licenceStatus = TIMED_OUT
    }
    return CaseLoadLicenceSummary(
      licenceStatus = licenceStatus,
      licenceType = licenceType,
      crn = deliusRecord.crn,
      nomisId = prisoner.prisonerNumber,
      name = name,
      releaseDate = nomisRecord.licenceStartDate,
      kind = licenceKind,
      hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder.licenceStartDate),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
        sentenceDateHolder,
      ),
    )
  }

  private fun findTimedOutLicenceToDisplay(licences: List<CaseLoadLicenceSummary>): CaseLoadLicenceSummary {
    val timedOutLicence = licences.find { it.licenceStatus == TIMED_OUT }!!

    if (timedOutLicence.versionOf != null) {
      val previouslyApproved = licences.find { licence -> licence.licenceId == timedOutLicence.versionOf }
      if (previouslyApproved != null) {
        return previouslyApproved.copy(
          licenceStatus = TIMED_OUT,
          licenceCreationType = LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
        )
      }
    }

    return timedOutLicence.copy(licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE)
  }

  private fun findHardStopLicenceToDisplay(licences: List<CaseLoadLicenceSummary>): CaseLoadLicenceSummary {
    val hardStopLicence = licences.find { it.kind == LicenceKind.HARD_STOP }!!

    if (hardStopLicence.licenceId == null || hardStopLicence.licenceStatus == IN_PROGRESS) {
      return hardStopLicence.copy(
        licenceStatus = TIMED_OUT,
        licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
      )
    }

    return hardStopLicence.copy(
      licenceStatus = TIMED_OUT,
      licenceCreationType = LicenceCreationType.LICENCE_CREATED_BY_PRISON,
    )
  }

  private fun findLicenceToDisplay(licences: List<CaseLoadLicenceSummary>): CaseLoadLicenceSummary {
    val licence: CaseLoadLicenceSummary = if (licences.size > 1) {
      licences.find { licence -> licence.licenceStatus !== APPROVED }!!
    } else {
      licences.first()
    }

    return if (licence.licenceId == null) {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED)
    } else {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS)
    }
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

  private fun transformLicenceSummaryToCaseLoadSummary(licenceSummary: LicenceSummary): CaseLoadLicenceSummary = CaseLoadLicenceSummary(
    licenceId = licenceSummary.licenceId,
    licenceStatus = licenceSummary.licenceStatus,
    kind = licenceSummary.kind,
    crn = licenceSummary.crn,
    nomisId = licenceSummary.nomisId,
    name = "${licenceSummary.forename} ${licenceSummary.surname}".convertToTitleCase().trim(),
    licenceType = licenceSummary.licenceType,
    comUsername = licenceSummary.comUsername,
    dateCreated = licenceSummary.dateCreated,
    approvedBy = licenceSummary.approvedByName,
    approvedDate = licenceSummary.approvedDate,
    versionOf = licenceSummary.versionOf,
    updatedByFullName = licenceSummary.updatedByFullName,
    hardStopWarningDate = licenceSummary.hardStopWarningDate,
    hardStopDate = licenceSummary.hardStopDate,
    licenceStartDate = licenceSummary.licenceStartDate,
    releaseDate = licenceSummary.licenceStartDate,
    isDueToBeReleasedInTheNextTwoWorkingDays = licenceSummary.isDueToBeReleasedInTheNextTwoWorkingDays,
    isReviewNeeded = licenceSummary.isReviewNeeded,
  )

  private fun filterHdcAndFutureReleases(
    cases: Map<ManagedOffenderCrn, CaseloadItem>,
    crnsToLicences: Map<String, CaseLoadLicenceSummary>,
  ): Map<ManagedOffenderCrn, CaseloadItem> {
    val hdcStatuses =
      hdcService.getHdcStatus(cases.map { (_, nomisRecord) -> nomisRecord.prisoner.toPrisonerSearchPrisoner() })
    return cases.filter { (deliusRecord, nomisRecord) ->
      val kind = crnsToLicences[deliusRecord.crn]!!.kind
      val bookingId = nomisRecord.prisoner.bookingId?.toLong()!!
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
    cases: Map<ManagedOffenderCrn, CaseloadItem>,
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
      hardStopWarningDate = licence.hardStopWarningDate,
      kind = licence.kind,
      licenceCreationType = licence.licenceCreationType,
      isReviewNeeded = licence.isReviewNeeded,
    )
  }.sortedWith(compareBy<ComCase> { it.releaseDate }.thenBy { it.name })

  private fun findExistingActiveAndPreReleaseLicences(crnList: List<String>): List<CaseLoadLicenceSummary> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceService.findLicencesForCrnsAndStatuses(
      crns = crnList,
      statusCodes = listOf(
        ACTIVE,
        IN_PROGRESS,
        SUBMITTED,
        APPROVED,
        TIMED_OUT,
      ),
    ).map { transformLicenceSummaryToCaseLoadSummary(it) }
  }
}
