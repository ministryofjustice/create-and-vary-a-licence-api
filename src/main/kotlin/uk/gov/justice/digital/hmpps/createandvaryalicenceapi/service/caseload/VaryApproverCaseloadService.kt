package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceVaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
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

  private fun findLicencesCasesForProbation(request: VaryApproverCaseloadSearchRequest) = when {
    request.probationPduCodes != null ->
      licenceCaseRepository.findSubmittedVariationsByPduCodes(request.probationPduCodes)

    request.probationAreaCode != null ->
      licenceCaseRepository.findSubmittedVariationsByRegion(request.probationAreaCode)

    else -> emptyList()
  }

  private fun searchLicences(request: VaryApproverCaseloadSearchRequest): Pair<List<LicenceVaryApproverCase>, List<LicenceVaryApproverCase>> {
    val pduLicences = if (request.probationPduCodes != null) {
      licenceCaseRepository.findSubmittedVariationsByPduCodes(request.probationPduCodes)
    } else {
      emptyList()
    }
    val regionLicences = if (request.probationAreaCode != null) {
      licenceCaseRepository.findSubmittedVariationsByRegion(request.probationAreaCode)
    } else {
      emptyList()
    }
    return pduLicences to regionLicences
  }

  private fun processLicences(
    cases: List<LicenceVaryApproverCase>,
    searchTerm: String?,
  ) = mapLicencesToOffenders(cases).run { applySearchFilter(this, searchTerm) }.run { applySort(this) }

  private fun mapLicencesToOffenders(licences: List<LicenceVaryApproverCase>): List<VaryApproverCase> {
    val prisonNumbers = licences.map { it.prisonNumber!! }
    val deliusRecords = deliusApiClient.getProbationCases(prisonNumbers).associateBy { it.nomisId }
    val nomisRecords =
      prisonerSearchApiClient.searchPrisonersByNomisIds(prisonNumbers).associateBy { it.prisonerNumber }
    val probationPractitioners = getProbationPractitioners(prisonNumbers)

    return licences.mapNotNull { licence ->
      val nomisRecord = nomisRecords[licence.prisonNumber]
      val deliusRecord = deliusRecords[licence.prisonNumber]
      val probationPractitioner = probationPractitioners[licence.prisonNumber?.lowercase()]!!
      if (nomisRecord == null || deliusRecord == null) {
        null
      } else {
        VaryApproverCase(
          licenceId = licence.licenceId,
          name = "${nomisRecord.firstName} ${nomisRecord.lastName}".trim()
            .convertToTitleCase(),
          crnNumber = licence.crn,
          licenceType = licence.typeCode,
          variationRequestDate = licence.dateCreated?.toLocalDate(),
          releaseDate = licence.licenceStartDate,
          probationPractitioner = probationPractitioner,
        )
      }
    }
  }

  private fun getProbationPractitioners(prisonNumbers: List<String>) = deliusApiClient.getOffenderManagersWithoutUser(prisonNumbers)
    .associate {
      val name = if (it.unallocated) "Not Allocated" else it.name.fullName()
      val staffCode = if (it.unallocated) null else it.code
      it.case.nomisId!!.lowercase() to ProbationPractitioner(
        staffCode = staffCode,
        name = name,
        allocated = !it.unallocated,
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
        case.probationPractitioner?.name?.lowercase()?.contains(searchString) ?: false
    }
  }

  private fun applySort(cases: List<VaryApproverCase>): List<VaryApproverCase> = cases.sortedBy { it.releaseDate }
}
