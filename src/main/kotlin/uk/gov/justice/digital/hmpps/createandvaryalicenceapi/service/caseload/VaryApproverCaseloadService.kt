package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class VaryApproverCaseloadService(
  private val caseloadService: CaseloadService,
  private val deliusApiClient: DeliusApiClient,
  private val licenceService: LicenceService,
) {
  fun getVaryApproverCaseload(varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest): List<VaryApproverCase> {
    val licences: List<LicenceSummary> = findLicences(varyApproverCaseloadSearchRequest)
    var cases = mapLicencesToOffenders(licences)
    cases = mapResponsibleComsToCases(cases)
    return buildCaseload(cases, varyApproverCaseloadSearchRequest.searchTerm)
  }

  fun findLicences(varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest) = if (varyApproverCaseloadSearchRequest.probationPduCodes != null) {
    licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        statusCodes = listOf(LicenceStatus.VARIATION_SUBMITTED),
        pdus = varyApproverCaseloadSearchRequest.probationPduCodes,
      ),
    )
  } else if (varyApproverCaseloadSearchRequest.probationAreaCode != null) {
    licenceService.findSubmittedVariationsByRegion(varyApproverCaseloadSearchRequest.probationAreaCode)
  } else {
    emptyList()
  }

  fun mapLicencesToOffenders(licences: List<LicenceSummary>): List<AcoCase> {
    val nomisIds = licences.map { it.nomisId }
    val deliusRecords = deliusApiClient.getProbationCases(nomisIds)
    val nomisRecords = caseloadService.getPrisonersByNumber(nomisIds)

    return licences.mapNotNull { licence ->
      val nomisRecord = nomisRecords.find { it.prisoner.prisonerNumber == licence.nomisId }
      val deliusRecord = deliusRecords.find { it.nomisId == licence.nomisId }
      if (nomisRecord != null && deliusRecord != null) {
        AcoCase(
          crn = deliusRecord.crn,
          nomisRecord = nomisRecord,
          licence = licence,
        )
      } else {
        null
      }
    }
  }

  private fun mapResponsibleComsToCases(caseload: List<AcoCase>): List<AcoCase> {
    val comUsernames = caseload.mapNotNull { it.licence.comUsername }

    val deliusStaffNames = deliusApiClient.getStaffDetailsByUsername(comUsernames)
    return caseload.map { case ->
      val responsibleCom = deliusStaffNames.find { com ->
        com.username?.lowercase() == case.licence.comUsername?.lowercase()
      }

      if (responsibleCom != null) {
        case.copy(probationPractitioner = responsibleCom.name?.fullName())
      } else {
        val coms = deliusApiClient.getOffenderManagers(caseload.map { it.crn })
        val com = coms.find { it.case.crn == case.crn }
        if (com == null || com.unallocated) {
          case
        } else {
          case.copy(probationPractitioner = com.name.fullName())
        }
      }
    }
  }

  private fun buildCaseload(cases: List<AcoCase>, searchTerm: String?): List<VaryApproverCase> {
    var caseload = cases.map { mapAcoCaseToView(it) }
    caseload = applySearchFilter(caseload, searchTerm)
    return applySort(caseload)
  }

  private fun mapAcoCaseToView(acoCase: AcoCase): VaryApproverCase {
    val licence = acoCase.licence
    return VaryApproverCase(
      licenceId = licence.licenceId,
      name = "${acoCase.nomisRecord.prisoner.firstName} ${acoCase.nomisRecord.prisoner.lastName}".trim()
        .convertToTitleCase(),
      crnNumber = acoCase.crn,
      licenceType = licence.licenceType,
      variationRequestDate = licence.dateCreated?.toLocalDate(),
      releaseDate = licence.licenceStartDate,
      probationPractitioner = acoCase.probationPractitioner,
    )
  }

  private fun applySearchFilter(cases: List<VaryApproverCase>, searchTerm: String?): List<VaryApproverCase> {
    if (searchTerm == null) {
      return cases
    }

    val searchString = searchTerm.lowercase().trim()

    return cases.filter { case ->
      case.crnNumber.lowercase().contains(searchString) ||
        case.name?.lowercase()?.contains(searchString) ?: false ||
        case.probationPractitioner?.lowercase()?.contains(searchString) ?: false
    }
  }
}

private fun applySort(cases: List<VaryApproverCase>): List<VaryApproverCase> = cases.sortedBy { it.releaseDate }

data class AcoCase(
  val crn: String,
  val nomisRecord: CaseloadItem,
  val licence: LicenceSummary,
  val probationPractitioner: String? = null,
)
