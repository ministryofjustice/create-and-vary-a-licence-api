package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class ApproverCaseloadService(
  private val prisonApproverService: PrisonApproverService,
  private val deliusApiClient: DeliusApiClient,
) {

  fun getApprovalNeeded(prisons: List<String>): List<ApprovalCase> {
    val licences = prisonApproverService.getLicencesForApproval(prisons.filterOutAdminPrisonCode())
    if (licences.isEmpty()) {
      return emptyList()
    }
    return createApprovalCaseload(licences)
  }

  fun getRecentlyApproved(prisons: List<String>): List<ApprovalCase> {
    val licences = prisonApproverService.findRecentlyApprovedLicences(prisons.filterOutAdminPrisonCode())
    if (licences.isEmpty()) {
      return emptyList()
    }
    return createApprovalCaseload(licences)
  }

  private fun List<String>.filterOutAdminPrisonCode() = filterNot { it == "CADM" }

  private fun createApprovalCaseload(licences: List<LicenceSummaryApproverView>): List<ApprovalCase> {
    val nomisIds = licences.mapNotNull { it.nomisId }
    val deliusRecords = deliusApiClient.getOffenderManagers(nomisIds)

    val prisonerRecord: List<Pair<CommunityManager, LicenceSummaryApproverView?>> =
      deliusRecords.map {
        Pair(it, licences.getLicenceSummary(it.case.nomisId!!))
      }

    val comUsernames = prisonerRecord.mapNotNull { (_, licenceSummary) -> licenceSummary?.comUsername }
    val deliusStaff = deliusApiClient.getStaffDetailsByUsername(comUsernames)

    return prisonerRecord.map { (activeCom, licenceSummary) ->
      ApprovalCase(
        probationPractitioner = findProbationPractitioner(licenceSummary?.comUsername, deliusStaff, activeCom),
        licenceId = licenceSummary?.licenceId,
        name = "${licenceSummary?.forename} ${licenceSummary?.surname}".convertToTitleCase(),
        prisonerNumber = licenceSummary?.nomisId,
        submittedByFullName = licenceSummary?.submittedByFullName,
        releaseDate = licenceSummary?.licenceStartDate,
        urgentApproval = licenceSummary?.isDueToBeReleasedInTheNextTwoWorkingDays,
        approvedBy = licenceSummary?.approvedByName,
        approvedOn = licenceSummary?.approvedDate,
        isDueForEarlyRelease = licenceSummary?.isDueForEarlyRelease,
        kind = licenceSummary?.kind,
      )
    }.sortedWith(compareBy(nullsFirst()) { it.releaseDate })
  }

  private fun List<LicenceSummaryApproverView>.getLicenceSummary(nomisId: String): LicenceSummaryApproverView? {
    val licenceSummaries = this.filter { it.nomisId == nomisId }
    return if (licenceSummaries.size == 1) licenceSummaries.first() else licenceSummaries.find { it.licenceStatus != LicenceStatus.ACTIVE }
  }

  fun findProbationPractitioner(
    comUsernameOnLicence: String?,
    deliusStaffNames: List<User>,
    activeCom: CommunityManager,
  ): ProbationPractitioner? {
    val responsibleCom = deliusStaffNames.find { com -> com.username?.lowercase() == comUsernameOnLicence?.lowercase() }
    return when {
      responsibleCom != null -> ProbationPractitioner(
        staffCode = responsibleCom.code,
        name = responsibleCom.name?.fullName()?.convertToTitleCase(),
      )

      activeCom.unallocated -> null

      else -> ProbationPractitioner(staffCode = activeCom.code, name = activeCom.name.fullName())
    }
  }
}
