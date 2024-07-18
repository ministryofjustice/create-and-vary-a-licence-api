package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseLoadLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComReviewCount
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.DeliusRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.isBreachOfTopUpSupervision
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.isRecall
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Service
class ComCaseloadService(
  private val caseloadService: CaseloadService,
  private val communityApiClient: CommunityApiClient,
  private val licenceService: LicenceService,
  private val prisonerSearchService: PrisonerSearchService,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val staffService: StaffService,
) {

  companion object {
    private const val PROBATION_SEARCH_BATCH_SIZE = 500
  }

  fun getStaffCreateCaseload(deliusStaffIdentifier: Long): List<ManagedCase> {
    val managedOffenders = communityApiClient.getManagedOffenders(deliusStaffIdentifier)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    val deliusAndNomisRecords = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    var caseload = filterOffendersEligibleForLicence(deliusAndNomisRecords)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildCreateCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return caseload
  }

  fun getTeamCreateCaseload(probationTeamCodes: List<String>, teamSelected: List<String>?): List<ManagedCase> {
    val teamCode = if (teamSelected?.isNotEmpty() == true) {
      teamSelected.first()
    } else {
      probationTeamCodes.first()
    }
    val managedOffenders = communityApiClient.getManagedOffendersByTeam(teamCode)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    var caseload = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    caseload = filterOffendersEligibleForLicence(caseload)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildCreateCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return caseload
  }

  fun getStaffVaryCaseload(deliusStaffIdentifier: Long): List<ManagedCase> {
    val managedOffenders = communityApiClient.getManagedOffenders(deliusStaffIdentifier)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    var caseload = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildVaryCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return caseload
  }

  fun getTeamVaryCaseload(probationTeamCodes: List<String>, teamSelected: List<String>?): List<ManagedCase> {
    val teamCode = if (teamSelected?.isNotEmpty() == true) {
      teamSelected.first()
    } else {
      probationTeamCodes.first()
    }
    val managedOffenders = communityApiClient.getManagedOffendersByTeam(teamCode)
    val managedOffenderToOffenderDetailMap = mapManagedOffenderRecordToOffenderDetail(managedOffenders)
    var caseload = pairDeliusRecordsWithNomis(managedOffenderToOffenderDetailMap)
    caseload = mapOffendersToLicences(caseload)
    caseload = buildVaryCaseload(caseload)
    caseload = mapResponsibleComsToCases(caseload)
    return caseload
  }

  fun getComReviewCount(deliusStaffIdentifier: Long): ComReviewCount =
    staffService.getReviewCounts(deliusStaffIdentifier)

  fun mapManagedOffenderRecordToOffenderDetail(caseload: List<ManagedOffenderCrn>): List<DeliusRecord> {
    val crns = caseload.map { c -> c.offenderCrn }
    val batchedCrns = crns.chunked(PROBATION_SEARCH_BATCH_SIZE)
    val batchedOffenders = batchedCrns.map { batch -> probationSearchApiClient.getOffendersByCrn(batch) }

    val offenders = batchedOffenders.flatten()
    return offenders.map { o ->
      DeliusRecord(o, caseload.find { c -> c.offenderCrn == o.otherIds.crn }!!)
    }
  }

  fun pairDeliusRecordsWithNomis(managedOffenders: List<DeliusRecord>): List<ManagedCase> {
    val caseloadNomisIds = managedOffenders.filter { offender -> offender.offenderDetail.otherIds.nomsNumber != null }
      .map { offender -> offender.offenderDetail.otherIds.nomsNumber }

    val nomisRecords = caseloadService.getPrisonersByNumber(caseloadNomisIds.filterNotNull())

    val records = managedOffenders.map { offender ->
      val caseLoadItem =
        nomisRecords.find { prisoner -> prisoner.prisoner.prisonerNumber == offender.offenderDetail.otherIds.nomsNumber }
      if (caseLoadItem != null) {
        ManagedCase(offender, nomisRecord = caseLoadItem.prisoner, cvlFields = caseLoadItem.cvl)
      } else {
        null
      }
    }
    return records.filterNotNull()
  }

  fun filterOffendersEligibleForLicence(cases: List<ManagedCase>): List<ManagedCase> {
    val eligibleOffenders = cases.filter { case ->
      case.nomisRecord?.prisonerNumber != null &&
        prisonerSearchService.getIneligibilityReasons(case.nomisRecord.prisonerNumber)
          .isEmpty()
    }
    return eligibleOffenders
  }

  fun mapOffendersToLicences(cases: List<ManagedCase>): List<ManagedCase> {
    val nomisIdList = cases.mapNotNull { offender -> offender.nomisRecord?.prisonerNumber }

    val existingLicences: List<LicenceSummary> = findExistingLicences(nomisIdList)

    return cases.map { case ->
      val updatedCase: ManagedCase
      val licences = existingLicences.filter { licence -> licence.nomisId == case.nomisRecord?.prisonerNumber }
      if (licences.isNotEmpty()) {
        updatedCase = case.copy(licences = licences.map { transformLicenceSummaryToCaseLoadSummary(it) })
      } else {
        // No licences present for this offender - determine how to show them in case lists
        // Determine the likely type of intended licence from the prison record
        val licenceType = LicenceType.getLicenceType(case.nomisRecord!!)

        // Default status (if not overridden below) will show the case as clickable on case lists
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
                hardStopDate = null,
                hardStopWarningDate = null,
                isDueToBeReleasedInTheNextTwoWorkingDays = false,
                releaseDate = null,
              ),
            ),
          )
        } else {
          val hardStopDate = case.cvlFields.hardStopDate
          val hardStopWarningDate = case.cvlFields.hardStopWarningDate
          val isDueToBeReleasedInTheNextTwoWorkingDays = case.cvlFields.isDueToBeReleasedInTheNextTwoWorkingDays
          val releaseDate = case.nomisRecord.confirmedReleaseDate ?: case.nomisRecord.conditionalReleaseDate
          updatedCase = case.copy(
            licences = listOf(
              CaseLoadLicenceSummary(
                licenceStatus = licenceStatus,
                licenceType = licenceType,
                hardStopDate = hardStopDate,
                hardStopWarningDate = hardStopWarningDate,
                isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
                releaseDate = releaseDate,
              ),
            ),
          )
        }
      }
      updatedCase
    }
  }

  fun transformLicenceSummaryToCaseLoadSummary(licenceSummary: LicenceSummary): CaseLoadLicenceSummary =
    CaseLoadLicenceSummary(
      licenceId = licenceSummary.licenceId,
      licenceStatus = licenceSummary.licenceStatus,
      kind = licenceSummary.kind,
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
      releaseDate = licenceSummary.actualReleaseDate ?: licenceSummary.conditionalReleaseDate,
      isDueToBeReleasedInTheNextTwoWorkingDays = licenceSummary.isDueToBeReleasedInTheNextTwoWorkingDays,
      isReviewNeeded = licenceSummary.isReviewNeeded,
    )

  fun buildCreateCaseload(managedOffenders: List<ManagedCase>): List<ManagedCase> =
    managedOffenders.filter { offender ->
      offender.nomisRecord?.status?.startsWith("ACTIVE") == true || offender.nomisRecord?.status == "INACTIVE TRN"
    }.filter { offender ->
      val releaseDate = offender.nomisRecord?.confirmedReleaseDate ?: offender.nomisRecord?.conditionalReleaseDate
      releaseDate?.isAfter(
        LocalDate.now().minusDays(1),
      ) ?: false
    }.filter { offender ->
      var licenceInValidState = offender.licences?.any { licence ->
        licence.licenceStatus in listOf(
          LicenceStatus.NOT_STARTED,
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.APPROVED,
          LicenceStatus.TIMED_OUT,
        )
      }

      if (licenceInValidState == false) {
        licenceInValidState = offender.nomisRecord.isBreachOfTopUpSupervision() || offender.nomisRecord.isRecall()
      }

      licenceInValidState ?: false
    }

  fun mapResponsibleComsToCasesWithExclusions(caseload: List<ManagedCase>): List<ManagedCase> {
    val comUsernames = caseload.map { case ->
      case.licences?.find { licence -> case.licences.size == 1 || licence.licenceStatus != LicenceStatus.ACTIVE }?.comUsername
    }.filterNotNull()

    val coms = communityApiClient.getStaffDetailsByUsername(comUsernames)
    return caseload.map { case ->
      val responsibleCom = coms.find { com ->
        com.username?.lowercase() == case.licences?.find { licence -> case.licences.size == 1 || licence.licenceStatus != LicenceStatus.ACTIVE }?.comUsername?.lowercase()
      }

      if (responsibleCom != null) {
        case.copy(
          probationPractitioner = ProbationPractitioner(
            responsibleCom.staffCode,
            name = "${responsibleCom.staff?.forenames} ${responsibleCom.staff?.surname}".convertToTitleCase(),
          ),
        )
      } else {
        if (case.deliusRecord?.managedOffenderCrn?.staff == null || case.deliusRecord.managedOffenderCrn.staff.unallocated == true) {
          case
        } else {
          case.copy(
            probationPractitioner = ProbationPractitioner(
              staffCode = case.deliusRecord.managedOffenderCrn.staff.code,
              name = "${case.deliusRecord.managedOffenderCrn.staff.forenames} ${case.deliusRecord.managedOffenderCrn.staff.surname}".trim(),
            ),
          )
        }
      }
    }
  }

  fun mapResponsibleComsToCases(caseload: List<ManagedCase>): List<ManagedCase> =
    mapResponsibleComsToCasesWithExclusions(caseload)

  fun buildVaryCaseload(managedOffenders: List<ManagedCase>): List<ManagedCase> = managedOffenders.filter { offender ->
    offender.licences?.any { licence ->
      licence.licenceStatus in listOf(
        LicenceStatus.ACTIVE,
        LicenceStatus.VARIATION_IN_PROGRESS,
        LicenceStatus.VARIATION_SUBMITTED,
        LicenceStatus.VARIATION_APPROVED,
        LicenceStatus.VARIATION_REJECTED,
      ) ||
        licence.isReviewNeeded ?: false
    } ?: false
  }

  fun findExistingLicences(nomisIdList: List<String>): List<LicenceSummary> = if (nomisIdList.isEmpty()) {
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
}
