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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManagerWithoutUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
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
    return createApprovalCaseload(licenceCases).sortedWith(byLicenceStartAndName)
  }

  fun getRecentlyApproved(prisons: List<String>): List<ApprovalCase> {
    val licenceCases = prisonApproverService.findRecentlyApprovedLicenceCases(prisons.filterOutAdminPrisonCode())
    return createApprovalCaseload(licenceCases).sortedWith(byApprovedOnAndName)
  }

  private fun List<String>.filterOutAdminPrisonCode() = filterNot { it == CENTRAL_ADMIN_CASELOAD }

  private fun createApprovalCaseload(licenceApproverCases: List<LicenceApproverCase>): List<ApprovalCase> {
    val prisonNumbers = licenceApproverCases.mapNotNull { it.prisonNumber }
    val deliusRecords = deliusApiClient.getOffenderManagersWithoutUser(prisonNumbers)
    val probationPractitioners = getProbationPractitioners(deliusRecords)

    return deliusRecords.mapNotNull { record ->
      val probationPractitioner =
        probationPractitioners[record.case.nomisId?.lowercase()] ?: ProbationPractitioner.UNALLOCATED
      val licence = licenceApproverCases.findLicenceToApprove(record.case.nomisId!!)
      when {
        licence == null -> null

        else ->
          ApprovalCase(
            probationPractitioner = probationPractitioner,
            licenceId = licence.licenceId,
            name = "${licence.forename} ${licence.surname}".convertToTitleCase(),
            prisonerNumber = licence.prisonNumber,
            submittedByFullName = licence.submittedByFullName,
            releaseDate = licence.licenceStartDate,
            urgentApproval = licence.isUrgentApproval(),
            approvedBy = licence.approvedByName,
            approvedOn = licence.approvedDate,
            kind = licence.kind,
            prisonCode = licence.prisonCode,
            prisonDescription = licence.prisonDescription,
          )
      }
    }
  }

  private fun LicenceApproverCase.isUrgentApproval() = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(this.licenceStartDate) || this.kind == LicenceKind.TIME_SERVED

  private fun getProbationPractitioners(coms: List<CommunityManagerWithoutUser>) = coms
    .associate {
      if (it.unallocated) {
        it.case.nomisId!!.lowercase() to ProbationPractitioner.unallocated(it.code)
      } else {
        it.case.nomisId!!.lowercase() to ProbationPractitioner(
          staffCode = it.code,
          name = it.name.fullName(),
          allocated = true,
        )
      }
    }

  private fun List<LicenceApproverCase>.findLicenceToApprove(prisonNumber: String): LicenceApproverCase? {
    val licenceSummaries = this.filter { it.prisonNumber == prisonNumber }
    return if (licenceSummaries.size == 1) licenceSummaries.first() else licenceSummaries.find { it.statusCode != ACTIVE }
  }

  private fun applySearch(cases: List<ApprovalCase>, searchString: String?): List<ApprovalCase> {
    if (searchString == null) {
      return cases
    }
    val term = searchString.lowercase()
    return cases.filter {
      it.name?.lowercase()?.contains(term) ?: false ||
        it.prisonerNumber?.lowercase()?.contains(term) ?: false ||
        it.probationPractitioner.name?.lowercase()?.contains(term) ?: false
    }
  }
}
