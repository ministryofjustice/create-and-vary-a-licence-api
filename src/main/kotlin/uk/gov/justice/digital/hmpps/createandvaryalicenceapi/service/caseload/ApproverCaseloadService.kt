package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ApproverSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ApproverSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDate

private const val CENTRAL_ADMIN_CASELOAD = "CADM"

@Service
class ApproverCaseloadService(
  private val prisonApproverService: PrisonApproverService,
  private val deliusApiClient: DeliusApiClient,
  private val releaseDateService: ReleaseDateService,
) {
  private val byApprovedOnAndName = compareByDescending<ApprovalCase> { it.approvedOn }
    .thenBy { it.name?.lowercase().orEmpty() }

  private val byLicenceStartAndName = compareBy<ApprovalCase, LocalDate?>(
    nullsFirst(naturalOrder()),
  ) { it.releaseDate }
    .thenBy { it.name?.lowercase().orEmpty() }

  fun searchForOffenderOnApproverCaseload(request: ApproverSearchRequest): ApproverSearchResponse {
    val approvalNeeded = getApprovalNeeded(request.prisonCaseloads).run { applySearch(this, request.query) }
    val recentlyApproved = getRecentlyApproved(request.prisonCaseloads).run { applySearch(this, request.query) }
    return ApproverSearchResponse(approvalNeeded, recentlyApproved)
  }

  fun getApprovalNeeded(prisons: List<String>): List<ApprovalCase> {
    val licenceCases = prisonApproverService.getLicenceCasesReadyForApproval(prisons.filterOutAdminPrisonCode())
    if (licenceCases.isEmpty()) {
      return emptyList()
    }
    return createApprovalCaseload(licenceCases).sortedWith(byLicenceStartAndName)
  }

  fun getRecentlyApproved(prisons: List<String>): List<ApprovalCase> {
    val licenceCases = prisonApproverService.findRecentlyApprovedLicenceCases(prisons.filterOutAdminPrisonCode())
    if (licenceCases.isEmpty()) {
      return emptyList()
    }
    return createApprovalCaseload(licenceCases).sortedWith(byApprovedOnAndName)
  }

  private fun List<String>.filterOutAdminPrisonCode() = filterNot { it == CENTRAL_ADMIN_CASELOAD }

  private fun createApprovalCaseload(licenceApproverCases: List<LicenceApproverCase>): List<ApprovalCase> {
    val prisonNumbers = licenceApproverCases.mapNotNull { it.prisonNumber }
    val deliusRecords = deliusApiClient.getOffenderManagers(prisonNumbers)

    val prisonerRecord: List<Pair<CommunityManager, LicenceApproverCase>> =
      deliusRecords.mapNotNull { record ->
        val licence = licenceApproverCases.getLicenceApproverCase(record.case.nomisId!!)
        if (licence != null) record to licence else null
      }

    val comUsernames = prisonerRecord.mapNotNull { (_, licenceApproverCase) -> licenceApproverCase.comUsername }
    val deliusStaff = deliusApiClient.getStaffDetailsByUsername(comUsernames)

    return prisonerRecord.map { (activeCom, licenceApproverCase) ->
      ApprovalCase(
        probationPractitioner = findProbationPractitioner(licenceApproverCase.comUsername, deliusStaff, activeCom),
        licenceId = licenceApproverCase.licenceId,
        name = "${licenceApproverCase.forename} ${licenceApproverCase.surname}".convertToTitleCase(),
        prisonerNumber = licenceApproverCase.prisonNumber,
        submittedByFullName = licenceApproverCase.submittedByFullName,
        releaseDate = licenceApproverCase.licenceStartDate,
        urgentApproval = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licenceApproverCase.licenceStartDate),
        approvedBy = licenceApproverCase.approvedByName,
        approvedOn = licenceApproverCase.approvedDate,
        kind = licenceApproverCase.kind,
        prisonCode = licenceApproverCase.prisonCode,
        prisonDescription = licenceApproverCase.prisonDescription,
      )
    }
  }

  private fun List<LicenceApproverCase>.getLicenceApproverCase(prisonNumber: String): LicenceApproverCase? {
    val licenceSummaries = this.filter { it.prisonNumber == prisonNumber }
    return if (licenceSummaries.size == 1) licenceSummaries.first() else licenceSummaries.find { it.statusCode != ACTIVE }
  }

  fun findProbationPractitioner(
    comUsernameOnLicence: String?,
    deliusStaffNames: List<StaffNameResponse>,
    activeCom: CommunityManager,
  ): ProbationPractitioner? {
    val responsibleCom = deliusStaffNames.find { com -> com.username?.lowercase() == comUsernameOnLicence?.lowercase() }
    return when {
      responsibleCom != null -> ProbationPractitioner(
        staffCode = responsibleCom.code,
        name = responsibleCom.name.fullName().convertToTitleCase(),
      )

      activeCom.unallocated -> null

      else -> ProbationPractitioner(staffCode = activeCom.code, name = activeCom.name.fullName())
    }
  }

  private fun applySearch(cases: List<ApprovalCase>, searchString: String?): List<ApprovalCase> {
    if (searchString == null) {
      return cases
    }
    val term = searchString.lowercase()
    return cases.filter {
      it.name?.lowercase()?.contains(term) ?: false ||
        it.prisonerNumber?.lowercase()?.contains(term) ?: false ||
        it.probationPractitioner?.name?.lowercase()?.contains(term) ?: false
    }
  }
}
