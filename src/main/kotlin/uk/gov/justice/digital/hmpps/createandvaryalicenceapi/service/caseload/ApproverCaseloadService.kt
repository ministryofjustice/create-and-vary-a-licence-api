package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ApproverCaseloadService(
  private val prisonApproverService: PrisonApproverService,
  private val probationSearchApiClient: ProbationSearchApiClient,
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
    val deliusRecords = probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)

    val prisonerRecord: List<Pair<OffenderDetail, LicenceSummaryApproverView?>> =
      deliusRecords.map {
        Pair(it, licences.getLicenceSummary(it))
      }

    val caseload = prisonerRecord.map { (deliusRecord, licenceSummary) ->
      ApprovalDetails(
        deliusRecord = deliusRecord,
        comUsernameOnLicence = licenceSummary?.comUsername,
        licenceSummary = ApprovalLicenceSummary(
          licenceId = licenceSummary?.licenceId,
          name = "${licenceSummary?.forename} ${licenceSummary?.surname}".convertToTitleCase(),
          prisonerNumber = licenceSummary?.nomisId,
          submittedByFullName = licenceSummary?.submittedByFullName,
          releaseDate = licenceSummary?.actualReleaseDate ?: licenceSummary?.conditionalReleaseDate,
          urgentApproval = licenceSummary?.isDueToBeReleasedInTheNextTwoWorkingDays,
          approvedBy = licenceSummary?.approvedByName,
          approvedOn = licenceSummary?.approvedDate,
          isDueForEarlyRelease = licenceSummary?.isDueForEarlyRelease,
        ),
      )
    }

    val comUsernames = caseload.mapNotNull { it.comUsernameOnLicence }
    val coms = deliusApiClient.getStaffDetailsByUsername(comUsernames)

    val approvalCases = caseload.map {
      ApprovalCase(
        licenceId = it.licenceSummary?.licenceId,
        name = it.licenceSummary?.name,
        prisonerNumber = it.licenceSummary?.prisonerNumber,
        submittedByFullName = it.licenceSummary?.submittedByFullName,
        releaseDate = it.licenceSummary?.releaseDate,
        urgentApproval = it.licenceSummary?.urgentApproval,
        approvedBy = it.licenceSummary?.approvedBy,
        approvedOn = it.licenceSummary?.approvedOn,
        isDueForEarlyRelease = it.licenceSummary?.isDueForEarlyRelease,
        probationPractitioner = findProbationPractitioner(it.deliusRecord, it.comUsernameOnLicence, coms),
      )
    }.sortedWith(compareBy(nullsFirst()) { it.releaseDate })

    return approvalCases
  }

  private fun List<LicenceSummaryApproverView>.getLicenceSummary(deliusRecord: OffenderDetail): LicenceSummaryApproverView? {
    val licenceSummaries = this.filter { it.nomisId == deliusRecord.otherIds.nomsNumber }
    return if (licenceSummaries.size == 1) licenceSummaries.first() else licenceSummaries.find { it.licenceStatus != LicenceStatus.ACTIVE }
  }

  fun findProbationPractitioner(
    deliusRecord: OffenderDetail?,
    comUsernameOnLicence: String?,
    coms: List<User>,
  ): ProbationPractitioner? {
    val responsibleCom = coms.find { com -> com.username?.lowercase() == comUsernameOnLicence?.lowercase() }

    return if (responsibleCom != null) {
      ProbationPractitioner(
        staffCode = responsibleCom.code,
        name = responsibleCom.name?.fullName()?.convertToTitleCase(),
      )
    } else {
      val activeCom = deliusRecord?.offenderManagers?.find { it.active }
      if (activeCom == null || activeCom.staffDetail.unallocated == true) {
        return null
      }
      ProbationPractitioner(
        staffCode = activeCom.staffDetail.code,
        name = "${activeCom.staffDetail.forenames} ${activeCom.staffDetail.surname}".convertToTitleCase(),
      )
    }
  }

  private data class ApprovalDetails(
    val deliusRecord: OffenderDetail?,
    val comUsernameOnLicence: String?,
    val licenceSummary: ApprovalLicenceSummary?,
  )

  private data class ApprovalLicenceSummary(
    val licenceId: Long?,
    val name: String?,
    val prisonerNumber: String?,
    val submittedByFullName: String?,
    val releaseDate: LocalDate?,
    val urgentApproval: Boolean?,
    val approvedBy: String?,
    val approvedOn: LocalDateTime?,
    val isDueForEarlyRelease: Boolean?,
  )
}
