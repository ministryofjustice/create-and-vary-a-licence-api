package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CaseAccessRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessRestrictionType

@Service
class PermissionsService(
  private val deliusApiClient: DeliusApiClient,
  private val licenceRepository: LicenceRepository,
) {

  fun getDetailsForExcludedOrRestrictedCase(crn: String): CaseAccessDetails {
    val username = SecurityContextHolder.getContext().authentication.name
    val caseAccessResponse = deliusApiClient.getCheckUserAccessForCRN(username, crn)

    return if (caseAccessResponse.userRestricted) {
      CaseAccessDetails(CaseAccessRestrictionType.RESTRICTED, caseAccessResponse.restrictionMessage)
    } else if (caseAccessResponse.userExcluded) {
      CaseAccessDetails(CaseAccessRestrictionType.EXCLUDED, caseAccessResponse.exclusionMessage)
    } else {
      CaseAccessDetails(CaseAccessRestrictionType.NONE)
    }
  }

  fun checkCaseAccess(caseAccessRequest: CaseAccessRequest): CaseAccessDetails {
    val username = SecurityContextHolder.getContext().authentication.name

    if (caseAccessRequest.crn == null && caseAccessRequest.nomisId == null && caseAccessRequest.licenceId == null) {
      throw ValidationException("crn, nomisId or licenceId must be provided")
    }

    val crn = caseAccessRequest.crn
      ?: if (caseAccessRequest.nomisId != null) {
        deliusApiClient.getProbationCase(caseAccessRequest.nomisId)?.crn
          ?: error("could not find a probation case for nomisId ${caseAccessRequest.nomisId}")
      } else {
        val licence = licenceRepository
          .findById(caseAccessRequest.licenceId!!)
          .orElseThrow { EntityNotFoundException("${caseAccessRequest.licenceId}") }
        licence.crn ?: error("licence with licenceId ${caseAccessRequest.licenceId} does not have a CRN")
      }

    val caseAccessResponse = deliusApiClient.getCheckUserAccessForCRN(username, crn)

    return if (caseAccessResponse.userRestricted) {
      CaseAccessDetails(CaseAccessRestrictionType.RESTRICTED, caseAccessResponse.restrictionMessage)
    } else if (caseAccessResponse.userExcluded) {
      CaseAccessDetails(CaseAccessRestrictionType.EXCLUDED, caseAccessResponse.exclusionMessage)
    } else {
      CaseAccessDetails(CaseAccessRestrictionType.NONE)
    }
  }
}
