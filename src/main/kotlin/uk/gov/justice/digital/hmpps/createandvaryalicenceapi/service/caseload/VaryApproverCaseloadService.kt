package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceVaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.VaryApproverCaseloadSearchResponse

@Service
class VaryApproverCaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val licenceCaseRepository: LicenceCaseRepository,
) {
  fun findLicencesCasesForProbation(varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest): List<LicenceVaryApproverCase> {
    if (varyApproverCaseloadSearchRequest.probationPduCodes != null) {
      return licenceCaseRepository.findSubmittedVariationsByPduCodes(probationPduCodes = varyApproverCaseloadSearchRequest.probationPduCodes)
    } else if (varyApproverCaseloadSearchRequest.probationAreaCode != null) {
      return licenceCaseRepository.findSubmittedVariationsByRegion(varyApproverCaseloadSearchRequest.probationAreaCode)
    }
    return emptyList()
  }

  fun searchLicences(varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest): Pair<List<LicenceVaryApproverCase>, List<LicenceVaryApproverCase>> {
    val pduLicences = if (varyApproverCaseloadSearchRequest.probationPduCodes != null) {
      licenceCaseRepository.findSubmittedVariationsByPduCodes(probationPduCodes = varyApproverCaseloadSearchRequest.probationPduCodes)
    } else {
      emptyList()
    }
    val regionLicences = if (varyApproverCaseloadSearchRequest.probationAreaCode != null) {
      licenceCaseRepository.findSubmittedVariationsByRegion(varyApproverCaseloadSearchRequest.probationAreaCode)
    } else {
      emptyList()
    }
    return Pair(pduLicences, regionLicences)
  }

  private fun mapLicencesToOffenders(licences: List<LicenceVaryApproverCase>): List<AcoCase> {
    val prisonNumbers = licences.map { it.prisonNumber!! }
    val deliusRecords = deliusApiClient.getProbationCases(prisonNumbers)
    val nomisRecords = prisonerSearchApiClient.searchPrisonersByNomisIds(prisonNumbers)

    return licences.mapNotNull { licence ->
      val nomisRecord = nomisRecords.find { it.prisonerNumber == licence.prisonNumber }
      val deliusRecord = deliusRecords.find { it.nomisId == licence.prisonNumber }
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

  private fun addProbationPractitionerCases(caseload: List<AcoCase>): List<AcoCase> {
    val comUsernames = caseload.mapNotNull { it.licence.comUsername }

    val deliusStaffNames = deliusApiClient.getStaffDetailsByUsername(comUsernames)
    return caseload.map { case ->
      val responsibleCom = deliusStaffNames.find { com ->
        com.username?.lowercase() == case.licence.comUsername?.lowercase()
      }

      if (responsibleCom != null) {
        case.copy(probationPractitioner = responsibleCom.name.fullName())
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
      name = "${acoCase.nomisRecord.firstName} ${acoCase.nomisRecord.lastName}".trim()
        .convertToTitleCase(),
      crnNumber = acoCase.crn,
      licenceType = licence.typeCode,
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

  private fun processLicences(
    licenceCases: List<LicenceVaryApproverCase>,
    searchTerm: String?,
  ): List<VaryApproverCase> {
    val cases = mapLicencesToOffenders(licenceCases)
    val enrichedCases = addProbationPractitionerCases(cases)
    return buildCaseload(enrichedCases, searchTerm)
  }

  fun getVaryApproverCaseload(varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest): List<VaryApproverCase> {
    val licenceCases = findLicencesCasesForProbation(varyApproverCaseloadSearchRequest)
    return processLicences(licenceCases, varyApproverCaseloadSearchRequest.searchTerm)
  }

  fun searchForOffenderOnVaryApproverCaseload(varyApproverCaseloadSearchRequest: VaryApproverCaseloadSearchRequest): VaryApproverCaseloadSearchResponse {
    val (pduLicences, regionLicences) = searchLicences(varyApproverCaseloadSearchRequest)
    val pduCasesResults = processLicences(pduLicences, varyApproverCaseloadSearchRequest.searchTerm)
    val regionCasesResults = processLicences(regionLicences, varyApproverCaseloadSearchRequest.searchTerm)
    return VaryApproverCaseloadSearchResponse(pduCasesResults, regionCasesResults)
  }
}

private fun applySort(cases: List<VaryApproverCase>): List<VaryApproverCase> = cases.sortedBy { it.releaseDate }

private data class AcoCase(
  val crn: String,
  val nomisRecord: PrisonerSearchPrisoner,
  val licence: LicenceVaryApproverCase,
  val probationPractitioner: String? = null,
)
