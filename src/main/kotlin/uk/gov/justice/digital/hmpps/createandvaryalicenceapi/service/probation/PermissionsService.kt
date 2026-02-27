package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.CheckCaseAccessRequest
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
      CaseAccessDetails(crn, CaseAccessRestrictionType.RESTRICTED, caseAccessResponse.restrictionMessage)
    } else if (caseAccessResponse.userExcluded) {
      CaseAccessDetails(crn, CaseAccessRestrictionType.EXCLUDED, caseAccessResponse.exclusionMessage)
    } else {
      CaseAccessDetails(crn, CaseAccessRestrictionType.NONE)
    }
  }

  fun checkCaseAccess(checkCaseAccessRequest: CheckCaseAccessRequest): CaseAccessDetails {
    val username = SecurityContextHolder.getContext().authentication.name

    if (checkCaseAccessRequest.crn == null && checkCaseAccessRequest.nomisId == null && checkCaseAccessRequest.licenceId == null) {
      throw ValidationException("crn, nomisId or licenceId must be provided")
    }

    val crn = checkCaseAccessRequest.crn
      ?: if (checkCaseAccessRequest.nomisId != null) {
        deliusApiClient.getProbationCase(checkCaseAccessRequest.nomisId)?.crn
          ?: error("could not find a probation case for nomisId ${checkCaseAccessRequest.nomisId}")
      } else {
        val licence = licenceRepository
          .findById(checkCaseAccessRequest.licenceId!!)
          .orElseThrow { EntityNotFoundException("${checkCaseAccessRequest.licenceId}") }
        licence.crn ?: error("licence with licenceId ${checkCaseAccessRequest.licenceId} does not have a CRN")
      }

    val caseAccessResponse = deliusApiClient.getCheckUserAccessForCRN(username, crn)

    return if (caseAccessResponse.userRestricted) {
      CaseAccessDetails(crn, CaseAccessRestrictionType.RESTRICTED, caseAccessResponse.restrictionMessage)
    } else if (caseAccessResponse.userExcluded) {
      CaseAccessDetails(crn, CaseAccessRestrictionType.EXCLUDED, caseAccessResponse.exclusionMessage)
    } else {
      CaseAccessDetails(crn, CaseAccessRestrictionType.NONE)
    }
  }
}
