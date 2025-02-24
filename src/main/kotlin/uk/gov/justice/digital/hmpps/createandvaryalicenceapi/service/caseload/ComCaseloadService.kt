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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toPrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Service
class ComCaseloadService(
  private val caseloadService: CaseloadService,
  private val deliusApiClient: DeliusApiClient,
  private val licenceService: LicenceService,
  private val comCaseloadSearchService: ComCaseloadSearchService,
  private val probationSearchApiClient: ProbationSearchApiClient,
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
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    var caseload = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildVaryCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return transformToVaryCaseload(caseload)
  }

  fun getTeamVaryCaseload(probationTeamCodes: List<String>, teamSelected: List<String>): List<ComCase> {
    val teamCode = getTeamCode(probationTeamCodes, teamSelected)
    val managedOffenders = deliusApiClient.getManagedOffendersByTeam(teamCode)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    var caseload = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildVaryCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return transformToVaryCaseload(caseload)
  }

  private fun mapManagedOffenderRecordToOffenderDetail(caseload: List<ManagedOffenderCrn>): List<DeliusRecord> {
    val crns = caseload.map { case -> case.crn }
    val offenders = probationSearchApiClient.getOffendersByCrn(crns)
    return offenders.map { offender ->
      DeliusRecord(offender, caseload.find { case -> case.crn == offender.otherIds.crn }!!)
    }
  }

  private fun pairDeliusRecordsWithNomis(managedOffenders: List<DeliusRecord>): List<ManagedCase> {
    val caseloadNomisIds = managedOffenders.filter { offender -> offender.offenderDetail.otherIds.nomsNumber != null }
      .mapNotNull { offender -> offender.offenderDetail.otherIds.nomsNumber }
    val nomisRecords = caseloadService.getPrisonersByNumber(caseloadNomisIds)

    val records = managedOffenders.map { offender ->
      val caseLoadItem =
        nomisRecords.find { prisoner -> prisoner.prisoner.prisonerNumber == offender.offenderDetail.otherIds.nomsNumber }
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
      case.nomisRecord?.prisonerNumber != null &&
        comCaseloadSearchService.getIneligibilityReasons(case.nomisRecord)
          .isEmpty()
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

        var licenceStatus = LicenceStatus.NOT_STARTED
        if (case.cvlFields.isInHardStopPeriod) {
          licenceStatus = LicenceStatus.TIMED_OUT
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
                hardStopDate = null,
                hardStopWarningDate = null,
                isDueToBeReleasedInTheNextTwoWorkingDays = false,
                releaseDate = null,
                isReviewNeeded = false,
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

  private fun buildCreateCaseload(managedOffenders: List<ManagedCase>): List<ManagedCase> = managedOffenders.filter { offender ->
    offender.nomisRecord?.status?.startsWith("ACTIVE") == true || offender.nomisRecord?.status == "INACTIVE TRN"
  }.filter { offender ->
    val releaseDate =
      offender.licences.find { licence -> offender.licences.size == 1 || licence.licenceStatus != LicenceStatus.ACTIVE }?.releaseDate
    releaseDate?.isAfter(
      LocalDate.now().minusDays(1),
    ) ?: false
  }.filter { offender ->
    offender.licences.any { licence ->
      licence.licenceStatus in listOf(
        LicenceStatus.NOT_STARTED,
        LicenceStatus.IN_PROGRESS,
        LicenceStatus.SUBMITTED,
        LicenceStatus.APPROVED,
        LicenceStatus.TIMED_OUT,
      )
    }
  }

  private fun mapResponsibleComsToCases(caseload: List<ManagedCase>): List<ManagedCase> {
    val comUsernames = caseload.mapNotNull { case ->
      case.licences.find { licence -> case.licences.size == 1 || licence.licenceStatus != LicenceStatus.ACTIVE }?.comUsername
    }

    val coms = deliusApiClient.getStaffDetailsByUsername(comUsernames)
    return caseload.map { case ->
      val responsibleCom = coms.find { com ->
        com.username?.lowercase() == case.licences.find { licence -> case.licences.size == 1 || licence.licenceStatus != LicenceStatus.ACTIVE }?.comUsername?.lowercase()
      }

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

  private fun buildVaryCaseload(managedOffenders: List<ManagedCase>): List<ManagedCase> = managedOffenders.filter { offender ->
    offender.licences.any { licence ->
      licence.licenceStatus in listOf(
        LicenceStatus.ACTIVE,
        LicenceStatus.VARIATION_IN_PROGRESS,
        LicenceStatus.VARIATION_SUBMITTED,
        LicenceStatus.VARIATION_APPROVED,
        LicenceStatus.VARIATION_REJECTED,
      ) ||
        licence.isReviewNeeded
    }
  }

  private fun findExistingLicences(nomisIdList: List<String>): List<LicenceSummary> = if (nomisIdList.isEmpty()) {
    emptyList()
  } else {
    licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        nomsIds = nomisIdList,
        statusCodes = listOf(
          LicenceStatus.ACTIVE,
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.APPROVED,
          LicenceStatus.VARIATION_IN_PROGRESS,
          LicenceStatus.VARIATION_SUBMITTED,
          LicenceStatus.VARIATION_APPROVED,
          LicenceStatus.VARIATION_REJECTED,
          LicenceStatus.TIMED_OUT,
        ),
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
    val timedOutLicence = case.licences.find { licence -> licence.licenceStatus === LicenceStatus.TIMED_OUT }
    val hardStopLicence = case.licences.find { licence -> licence.kind === LicenceKind.HARD_STOP }

    if (timedOutLicence?.versionOf != null) {
      val previouslyApproved = case.licences.find { licence -> licence.licenceId == timedOutLicence.versionOf }
      if (previouslyApproved != null) {
        return previouslyApproved.copy(
          licenceStatus = LicenceStatus.TIMED_OUT,
          licenceCreationType = LicenceCreationType.LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
        )
      }
    }

    if ((timedOutLicence != null && hardStopLicence == null) || hardStopLicence?.licenceStatus === LicenceStatus.IN_PROGRESS
    ) {
      if (timedOutLicence != null) {
        return timedOutLicence.copy(licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE)
      }

      if (hardStopLicence != null) {
        return hardStopLicence.copy(
          licenceStatus = LicenceStatus.TIMED_OUT,
          licenceCreationType = LicenceCreationType.PRISON_WILL_CREATE_THIS_LICENCE,
        )
      }
    }

    if (hardStopLicence != null) {
      return hardStopLicence.copy(
        licenceStatus = LicenceStatus.TIMED_OUT,
        licenceCreationType = LicenceCreationType.LICENCE_CREATED_BY_PRISON,
      )
    }

    val licence: CaseLoadLicenceSummary = if (case.licences.size > 1) {
      case.licences.find { licence -> licence.licenceStatus !== LicenceStatus.APPROVED }!!
    } else {
      case.licences.first()
    }

    return if (licence.licenceId == null) {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_NOT_STARTED)
    } else {
      licence.copy(licenceCreationType = LicenceCreationType.LICENCE_IN_PROGRESS)
    }
  }

  private fun transformToVaryCaseload(caseload: List<ManagedCase>): List<ComCase> = caseload.map { managedCase ->
    val licences = managedCase.licences.filter { licence -> licence.licenceStatus != LicenceStatus.TIMED_OUT }
    val licence = if (licences.size > 1) {
      licences.find { licence -> licence.licenceStatus != LicenceStatus.ACTIVE && !licence.isReviewNeeded }
    } else {
      licences.first()
    }

    ComCase(
      licenceId = licence?.licenceId,
      licenceType = licence?.licenceType,
      licenceStatus = licence?.licenceStatus,
      crnNumber = licence?.crn,
      prisonerNumber = licence?.nomisId,
      kind = licence?.kind,
      name = licence?.name,
      releaseDate = licence?.releaseDate,
      probationPractitioner = managedCase.probationPractitioner,
      isDueForEarlyRelease = managedCase.cvlFields.isDueForEarlyRelease,
      isReviewNeeded = licence?.isReviewNeeded ?: false,
    )
  }

  private fun getLicenceStartDates(casesToLicences: Map<ManagedCase, List<LicenceSummary>>): Map<String, LocalDate?> {
    val prisonerSearchPrisonersWithoutLicences = casesToLicences.filter { (_, licences) ->
      licences.isEmpty()
    }.map { (case, _) -> case.nomisRecord!!.toPrisonerSearchPrisoner() }

    return releaseDateService.getLicenceStartDates(prisonerSearchPrisonersWithoutLicences)
  }
}
