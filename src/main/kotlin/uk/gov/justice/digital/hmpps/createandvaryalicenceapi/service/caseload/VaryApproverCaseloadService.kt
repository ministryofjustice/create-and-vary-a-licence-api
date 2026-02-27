package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse.Companion.unrestricted
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.VaryApproverCaseloadSearchResponse

@Service
class VaryApproverCaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val licenceCaseRepository: LicenceCaseRepository,
  @param:Value("\${feature.toggle.lao.enabled}") private val laoEnabled: Boolean = false,
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
    val crns = deliusRecords.values.map { it.crn }.distinct()
    val caseAccessRecords = if (laoEnabled) {
      getCaseAccessRecords(crns)
    } else {
      emptyMap()
    }

    return licences.mapNotNull { licence ->
      val nomisRecord = nomisRecords[licence.prisonNumber]
      val deliusRecord = deliusRecords[licence.prisonNumber]
      val caseAccessRecord = caseAccessRecords[deliusRecord?.crn] ?: unrestricted
      val isLao = caseAccessRecord.userExcluded || caseAccessRecord.userRestricted
      val probationPractitioner = probationPractitioners[licence.prisonNumber?.lowercase()] ?: ProbationPractitioner.UNALLOCATED

      when {
        nomisRecord == null || deliusRecord == null -> null
        isLao -> VaryApproverCase.restrictedCase(licence)
        else -> VaryApproverCase(
          licenceId = licence.licenceId,
          name = "${nomisRecord.firstName} ${nomisRecord.lastName}".trim()
            .convertToTitleCase(),
          crnNumber = licence.crn,
          licenceType = licence.typeCode,
          variationRequestDate = licence.dateCreated?.toLocalDate(),
          releaseDate = licence.licenceStartDate,
          probationPractitioner = probationPractitioner,
          isLao = false,
        )
      }
    }
  }

  private fun getProbationPractitioners(prisonNumbers: List<String>) = deliusApiClient.getOffenderManagersWithoutUser(prisonNumbers)
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

  private fun applySearchFilter(cases: List<VaryApproverCase>, searchTerm: String?): List<VaryApproverCase> {
    if (searchTerm == null) {
      return cases
    }

    val searchString = searchTerm.lowercase().trim()

    return cases.filter { case ->
      if (case.isLao && !case.crnNumber.contains(searchString, ignoreCase = true)) return@filter false
      case.crnNumber.lowercase().contains(searchString) ||
        case.name?.lowercase()?.contains(searchString) ?: false ||
        case.probationPractitioner.name?.lowercase()?.contains(searchString) ?: false
    }
  }

  private fun applySort(cases: List<VaryApproverCase>): List<VaryApproverCase> = cases.sortedBy { it.releaseDate }

  private fun getCaseAccessRecords(crns: List<String>): Map<String, CaseAccessResponse> {
    val username = SecurityContextHolder.getContext().authentication.name
    return deliusApiClient.getCheckUserAccess(username, crns).associateBy { it.crn }
  }
}
