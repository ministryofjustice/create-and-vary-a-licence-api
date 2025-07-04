package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceCreationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.DeliusRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_REJECTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Service
class ComCaseloadService(
  private val caseloadService: CaseloadService,
  private val deliusApiClient: DeliusApiClient,
  private val licenceService: LicenceService,
  private val eligibilityService: EligibilityService,
  private val hdcService: HdcService,
  private val releaseDateService: ReleaseDateService,
) {

  fun getStaffCreateCaseload(deliusStaffIdentifier: Long): List<ComCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    val deliusAndNomisRecords = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    var caseload = filterOffendersEligibleForLicence(deliusAndNomisRecords)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildCreateCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return transformToCreateCaseload(caseload)
  }

  fun getTeamCreateCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    var caseload = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    caseload = filterOffendersEligibleForLicence(caseload)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildCreateCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return transformToCreateCaseload(caseload)
  }

  fun getStaffVaryCaseload(deliusStaffIdentifier: Long): List<ComCase> {
    val managedOffenders = deliusApiClient.getManagedOffenders(deliusStaffIdentifier)
    val casesToLicences = mapCaseToVaryLicence(managedOffenders)
    return transformToVaryCaseload(casesToLicences)
  }

  fun getTeamVaryCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val casesToLicences = mapCaseToVaryLicence(managedOffenders)
    return transformToVaryCaseload(casesToLicences)
  }

  private fun mapManagedOffenderRecordToOffenderDetail(caseload: List<ManagedOffenderCrn>): List<DeliusRecord> {
    val crns = caseload.mapNotNull { case -> case.crn }
    val offenders = deliusApiClient.getProbationCases(crns)
    return offenders.map { offender ->
      DeliusRecord(offender, caseload.find { case -> case.crn == offender.crn }!!)
    }
  }

  private fun pairDeliusRecordsWithNomis(managedOffenders: List<DeliusRecord>): List<ManagedCase> {
    val caseloadNomisIds = managedOffenders.filter { offender -> offender.probationCase.nomisId != null }
      .mapNotNull { offender -> offender.probationCase.nomisId }
    val nomisRecords = caseloadService.getPrisonersByNumber(caseloadNomisIds)

    val records = managedOffenders.map { offender ->
      val caseLoadItem =
        nomisRecords.find { prisoner -> prisoner.prisoner.prisonerNumber == offender.probationCase.nomisId }
      if (caseLoadItem != null) {
        ManagedCase(
          offender,
          nomisRecord = caseLoadItem.prisoner,
          cvlFields = caseLoadItem.cvl,
        )
      } else {
        null
      }
    }
    return records.filterNotNull()
  }

  private fun filterOffendersEligibleForLicence(cases: List<ManagedCase>): List<ManagedCase> {
    val eligibleOffenders = cases.filter { case ->
      val prisoner = case.nomisRecord?.toPrisonerSearchPrisoner()
      when {
        prisoner == null || prisoner.bookingId == null -> false
        !eligibilityService.isEligibleForCvl(prisoner) -> false
        else -> true
      }
    }
    return eligibleOffenders
  }

  fun mapOffendersToLicences(cases: List<ManagedCase>): List<ManagedCase> {
    val nomisIdList = cases.mapNotNull { offender -> offender.nomisRecord?.prisonerNumber }
    val existingLicences = findExistingLicences(nomisIdList).groupBy { it.nomisId }
    val casesToLicences = cases.filter { it.nomisRecord != null }.associateWith {
      existingLicences[it.nomisRecord!!.prisonerNumber] ?: emptyList()
    }
    val licenceStartDates = getLicenceStartDates(casesToLicences)

    return casesToLicences.map { (case, licences) ->
      val updatedCase: ManagedCase
      if (licences.isNotEmpty()) {
        updatedCase = case.copy(
          licences = licences.map { transformLicenceSummaryToCaseLoadSummary(it) },
        )
      } else {
        // No licences present for this offender - determine how to show them in case lists
        val licenceType = LicenceType.getLicenceType(case.nomisRecord!!)
        val name = "${case.nomisRecord.firstName} ${case.nomisRecord.lastName}".trim().convertToTitleCase()

        var licenceStatus = NOT_STARTED
        if (case.cvlFields.isInHardStopPeriod) {
          licenceStatus = TIMED_OUT
        }

        if (case.nomisRecord.conditionalReleaseDate == null) {
          updatedCase = case.copy(
            licences = listOf(
              CaseLoadLicenceSummary(
                licenceStatus = licenceStatus,
                licenceType = licenceType,
                crn = case.deliusRecord?.managedOffenderCrn?.crn,
                nomisId = case.nomisRecord.prisonerNumber,
                name = name,
              ),
            ),
          )
        } else {
          updatedCase = case.copy(
            licences = listOf(
              CaseLoadLicenceSummary(
                licenceStatus = licenceStatus,
                licenceType = licenceType,
                crn = case.deliusRecord?.managedOffenderCrn?.crn,
                nomisId = case.nomisRecord.prisonerNumber,
                name = name,
                hardStopDate = case.cvlFields.hardStopDate,
                hardStopWarningDate = case.cvlFields.hardStopWarningDate,
                isDueToBeReleasedInTheNextTwoWorkingDays = case.cvlFields.isDueToBeReleasedInTheNextTwoWorkingDays,
                releaseDate = licenceStartDates[case.nomisRecord.prisonerNumber],
                isReviewNeeded = false,
              ),
            ),
          )
        }
      }
      updatedCase
    }
  }

  fun mapCaseToVaryLicence(cases: List<ManagedOffenderCrn>): Map<ManagedOffenderCrn, LicenceSummary> {
    val licences = findExistingActiveAndVariationLicences(cases.mapNotNull { it.crn })
    return cases.mapNotNull { case ->
      val caseLicences = licences.filter { licence -> case.crn == licence.crn }
      val varyLicence = findVaryLicenceToDisplay(caseLicences)
      when {
        varyLicence == null -> null
        else -> case to varyLicence
      }
    }.toMap()
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

  private fun buildCreateCaseload(managedOffenders: List<ManagedCase>): List<ManagedCase> {
    val hdcStatuses = hdcService.getHdcStatus(managedOffenders.map { it.nomisRecord!!.toPrisonerSearchPrisoner() })

    return managedOffenders.filter {
      val kind = it.findRelevantLicence()?.kind
      val bookingId = it.nomisRecord?.bookingId?.toLong()!!
      hdcStatuses.canBeSeenByCom(kind, bookingId)
    }.filter { offender ->
      offender.nomisRecord?.status?.startsWith("ACTIVE") == true || offender.nomisRecord?.status == "INACTIVE TRN"
    }.filter { offender ->
      offender.findRelevantLicence()?.releaseDate?.isAfter(LocalDate.now().minusDays(1)) == true
    }.filter { offender ->
      offender.licences.any { licence ->
        licence.licenceStatus in listOf(
          NOT_STARTED,
          IN_PROGRESS,
          SUBMITTED,
          APPROVED,
          TIMED_OUT,
        )
      }
    }
  }

  private fun mapResponsibleComsToCases(caseload: List<ManagedCase>): List<ManagedCase> {
    val comUsernames = caseload.mapNotNull { it.findRelevantLicence()?.comUsername }.distinct()

    val coms = deliusApiClient.getStaffDetailsByUsername(comUsernames).associateBy { it.username?.lowercase() }
    return caseload.map { case ->
      val responsibleCom = coms[case.findRelevantLicence()?.comUsername?.lowercase()]
      if (responsibleCom != null) {
        case.copy(
          probationPractitioner = ProbationPractitioner(
            responsibleCom.code,
            name = responsibleCom.name?.fullName(),
          ),
        )
      } else {
        if (case.deliusRecord?.managedOffenderCrn?.staff == null || case.deliusRecord.managedOffenderCrn.staff.unallocated == true) {
          case
        } else {
          case.copy(
            probationPractitioner = ProbationPractitioner(
              staffCode = case.deliusRecord.managedOffenderCrn.staff.code,
              name = case.deliusRecord.managedOffenderCrn.staff.name?.fullName(),
            ),
          )
        }
      }
    }
  }

  private fun getComDetails(casesToLicences: Map<ManagedOffenderCrn, LicenceSummary>): Map<String?, ProbationPractitioner> {
    val comUsernames = casesToLicences.mapNotNull { (_, licence) -> licence.comUsername }.distinct()
    val coms = deliusApiClient.getStaffDetailsByUsername(comUsernames)
    return casesToLicences.map { (case, licence) ->
      val com = coms.find { c -> licence.comUsername?.lowercase() == c.username?.lowercase() }
      when {
        com != null -> case.crn to ProbationPractitioner(
          com.code,
          name = com.name?.fullName(),
          staffUsername = com.username,
        )

        case.staff == null || case.staff.unallocated == true -> case.crn to ProbationPractitioner()
        else -> case.crn to ProbationPractitioner(
          staffCode = case.staff.code,
          name = case.staff.name?.fullName(),
        )
      }
    }.toMap()
  }

  private fun findExistingLicences(nomisIdList: List<String>): List<LicenceSummary> = if (nomisIdList.isEmpty()) {
    emptyList()
  } else {
    licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        nomsIds = nomisIdList,
        statusCodes = listOf(
          ACTIVE,
          IN_PROGRESS,
          SUBMITTED,
          APPROVED,
          VARIATION_IN_PROGRESS,
          VARIATION_SUBMITTED,
          VARIATION_APPROVED,
          VARIATION_REJECTED,
          TIMED_OUT,
        ),
      ),
    )
  }

  private fun findExistingActiveAndVariationLicences(crnList: List<String>): List<LicenceSummary> = if (crnList.isEmpty()) {
    emptyList()
  } else {
    licenceService.findLicencesForCrnsAndStatuses(
      crns = crnList,
      statusCodes = listOf(
        ACTIVE,
        VARIATION_IN_PROGRESS,
        VARIATION_SUBMITTED,
        VARIATION_APPROVED,
        VARIATION_REJECTED,
      ),
    )
  }

  private fun getTeamCode(probationTeamCodes: List<String>, teamSelected: List<String>): String = if (teamSelected.isNotEmpty()) {
    teamSelected.first()
  } else {
    probationTeamCodes.first()
  }

  private fun transformToCreateCaseload(caseload: List<ManagedCase>): List<ComCase> = caseload.map { managedCase ->
    val licence = this.findLicenceToDisplay(managedCase)
    ComCase(
      licenceId = licence.licenceId,
      licenceStatus = licence.licenceStatus,
      licenceType = licence.licenceType,
      name = licence.name,
      crnNumber = licence.crn,
      prisonerNumber = licence.nomisId,
      releaseDate = licence.releaseDate,
      probationPractitioner = managedCase.probationPractitioner,
      hardStopDate = licence.hardStopDate,
      hardStopWarningDate = licence.hardStopWarningDate,
      kind = licence.kind,
      isDueForEarlyRelease = managedCase.cvlFields.isDueForEarlyRelease,
      licenceCreationType = licence.licenceCreationType,
      isReviewNeeded = licence.isReviewNeeded,
    )
  }.sortedBy { it.releaseDate }

  private fun findLicenceToDisplay(case: ManagedCase): CaseLoadLicenceSummary {
    val timedOutLicence = case.licences.find { licence -> licence.licenceStatus === TIMED_OUT }
    val hardStopLicence = case.licences.find { licence -> licence.kind === LicenceKind.HARD_STOP }

    if (timedOutLicence?.versionOf != null) {
      val previouslyApproved = case.licences.find { licence -> licence.licenceId == timedOutLicence.versionOf }
      if (previouslyApproved != null) {
        return previouslyApproved.copy(
          licenceStatus = TIMED_OUT,
          licenceCreationType = LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
        )
      }
    }

    if ((timedOutLicence != null && hardStopLicence == null) || hardStopLicence?.licenceStatus == IN_PROGRESS) {
      if (timedOutLicence != null) {
        return timedOutLicence.copy(licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE)
      }

      if (hardStopLicence != null) {
        return hardStopLicence.copy(
          licenceStatus = TIMED_OUT,
          licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
        )
      }
    }

    if (hardStopLicence != null) {
      return hardStopLicence.copy(
        licenceStatus = TIMED_OUT,
        licenceCreationType = LicenceCreationType.LICENCE_CREATED_BY_PRISON,
      )
    }

    val licence: CaseLoadLicenceSummary = if (case.licences.size > 1) {
      case.licences.find { licence -> licence.licenceStatus !== APPROVED }!!
    } else {
      case.licences.first()
    }

    return if (licence.licenceId == null) {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED)
    } else {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS)
    }
  }

  private fun transformToVaryCaseload(casesToLicences: Map<ManagedOffenderCrn, LicenceSummary>): List<ComCase> {
    val comDetails = getComDetails(casesToLicences)
    return casesToLicences.map { (case, licence) ->
      ComCase(
        licenceId = licence.licenceId,
        licenceType = licence.licenceType,
        licenceStatus = licence.licenceStatus,
        crnNumber = licence.crn,
        prisonerNumber = licence.nomisId,
        kind = licence.kind,
        name = "${licence.forename} ${licence.surname}".trim().convertToTitleCase(),
        releaseDate = licence.licenceStartDate,
        probationPractitioner = comDetails[case.crn],
        isDueForEarlyRelease = false,
        isReviewNeeded = licence.isReviewNeeded,
      )
    }
  }

  private fun findVaryLicenceToDisplay(licences: List<LicenceSummary>): LicenceSummary? = when {
    licences.isEmpty() -> null
    licences.size > 1 -> licences.find { licence -> licence.licenceStatus != ACTIVE && !licence.isReviewNeeded }
    else -> licences.first()
  }

  private fun ManagedCase.findRelevantLicence() = licences.find { licence -> licences.size == 1 || licence.licenceStatus != ACTIVE }

  private fun getLicenceStartDates(casesToLicences: Map<ManagedCase, List<LicenceSummary>>): Map<String, LocalDate?> {
    val prisonerSearchPrisonersWithoutLicences = casesToLicences.filter { (_, licences) ->
      licences.isEmpty()
    }.map { (case, _) -> case.nomisRecord!!.toPrisonerSearchPrisoner() }

    return prisonerSearchPrisonersWithoutLicences.associate {
      val licenceKind = caseloadService.determineLicenceKind(it)
      val licenceStartDate = releaseDateService.getLicenceStartDate(it, licenceKind)
      it.prisonerNumber to licenceStartDate
    }
  }
}
