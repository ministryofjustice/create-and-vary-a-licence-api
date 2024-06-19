package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummaryApproverView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonApproverService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ApproverCaseloadService(
  private val prisonApproverService: PrisonApproverService,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val communityApiClient: CommunityApiClient,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getApprovalNeeded(prisons: List<String>): List<ApprovalCase> {
    val filteredPrisons = prisons.filterNot { it == "CADM" }
    val licences = prisonApproverService.getLicencesForApproval(filteredPrisons)
    if (licences.isEmpty()) {
      return emptyList()
    }

    val nomisIds = licences.mapNotNull { it.nomisId }
    val deliusRecords = probationSearchApiClient.searchForPeopleByNomsNumber(nomisIds)
    val deliusNomisIds = deliusRecords.mapNotNull { it.otherIds.nomsNumber }
    val nomisRecords = prisonerSearchApiClient.searchPrisonersByNomisIds(deliusNomisIds)

    val prisonerRecord: List<Triple<PrisonerSearchPrisoner, OffenderDetail?, LicenceSummaryApproverView?>> =
      nomisRecords.map {
        Triple(it, getDeliusRecord(it, deliusRecords), getLicenceSummary(it, licences))
      }

    val caseload = prisonerRecord.map { (nomisRecord, deliusRecord, licenceSummary) ->
      val releaseDate = when (licenceSummary) {
        null -> selectReleaseDate(nomisRecord) // you would always have a licence summary here as without it you wouldnt get the nomis ID
        else -> licenceSummary.actualReleaseDate ?: licenceSummary.conditionalReleaseDate
      }
      ApprovalDetails(
        deliusRecord = deliusRecord,
        comUsernameOnLicence = licenceSummary?.comUsername,
        licenceSummary = ApprovalLicenceSummary(
          licenceId = licenceSummary?.licenceId,
          name = "${nomisRecord.firstName} ${nomisRecord.lastName}".convertToTitleCase(),
          prisonerNumber = nomisRecord.prisonerNumber,
          submittedByFullName = licenceSummary?.submittedByFullName,
          releaseDate = releaseDate,
          urgentApproval = licenceSummary?.isDueToBeReleasedInTheNextTwoWorkingDays,
          approvedBy = licenceSummary?.approvedByName,
          approvedOn = licenceSummary?.approvedDate,
          isDueForEarlyRelease = licenceSummary?.isDueForEarlyRelease,
        ),
      )
    }

    val comUsernames = caseload.mapNotNull { it.comUsernameOnLicence }
    val coms = communityApiClient.getStaffDetailsByUsername(comUsernames)

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
    }.sortedByDescending { it.releaseDate }

    return approvalCases
  }
}

private fun getDeliusRecord(nomisRecord: PrisonerSearchPrisoner, deliusRecords: List<OffenderDetail>): OffenderDetail? {
  return deliusRecords.find { it.otherIds.nomsNumber == nomisRecord.prisonerNumber }
}

private fun getLicenceSummary(nomisRecord: PrisonerSearchPrisoner, licences: List<LicenceSummaryApproverView>): LicenceSummaryApproverView? {
  val licenceSummaries = licences.filter { it.nomisId == nomisRecord.prisonerNumber }
  return if (licenceSummaries.size == 1) licenceSummaries.first() else licenceSummaries.find { it.licenceStatus != LicenceStatus.ACTIVE }
}

private fun selectReleaseDate(nomisRecord: PrisonerSearchPrisoner): LocalDate? {
  return nomisRecord.confirmedReleaseDate ?: nomisRecord.conditionalReleaseDate
}

private fun findProbationPractitioner(deliusRecord: OffenderDetail?, comUsernameOnLicence: String?, coms: List<User>): ProbationPractitioner? {
  val responsibleCom = coms.find { com -> com.username?.lowercase() == comUsernameOnLicence?.lowercase() }

  return if (responsibleCom != null) {
    ProbationPractitioner(
      staffCode = responsibleCom.staffCode,
      name = "${responsibleCom.staff?.forenames} ${responsibleCom.staff?.surname}".convertToTitleCase(),
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

data class ApprovalDetails(
  val deliusRecord: OffenderDetail?,
  val comUsernameOnLicence: String?,
  val licenceSummary: ApprovalLicenceSummary?,
)

data class ApprovalLicenceSummary(
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
