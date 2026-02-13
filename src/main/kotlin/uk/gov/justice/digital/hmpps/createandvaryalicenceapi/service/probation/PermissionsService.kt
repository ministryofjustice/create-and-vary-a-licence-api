package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessRestrictionType

@Service
class PermissionsService(private val deliusApiClient: DeliusApiClient) {

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
}
