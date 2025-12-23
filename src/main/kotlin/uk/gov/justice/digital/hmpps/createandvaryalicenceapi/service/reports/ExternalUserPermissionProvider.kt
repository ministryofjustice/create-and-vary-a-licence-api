package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.coerce404ToEmptyOrThrow
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.UserPermissionProvider
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.WARNING_NO_ACTIVE_CASELOAD
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

@Component
class ExternalUserPermissionProvider(@Qualifier("oauthManageUsersWebClient") private val manageUsersWebClient: WebClient) : UserPermissionProvider {

  companion object {
    private val logger = LoggerFactory.getLogger(ExternalUserPermissionProvider::class.java)

    data class RolesResponse(val roleCode: String)

    private val ROLES: ParameterizedTypeReference<List<RolesResponse>> =
      object : ParameterizedTypeReference<List<RolesResponse>>() {}
  }

  // This is not currently called, will need to be changed in the library to allow returning null
  override fun getActiveCaseloadId(username: String): String {
    val caseloadResponse = getPrisonUsersCaseload(username)

    if (caseloadResponse.accountType != "GENERAL") {
      throw NoDataAvailableException("'${caseloadResponse.accountType}' account types are currently not supported.")
    }

    if (caseloadResponse.activeCaseload == null) {
      throw NoDataAvailableException(WARNING_NO_ACTIVE_CASELOAD)
    }

    return (caseloadResponse.activeCaseload as Caseload).id
  }

  override fun getCaseloads(username: String): List<Caseload> {
    val caseloadResponse = getPrisonUsersCaseload(username)

    // Will need to remove this check when external users are supported
    //    if (caseloadResponse.caseloads.isEmpty()) {
    //      throw NoDataAvailableException(WARNING_NO_CASELOADS)
    //    }

    return caseloadResponse.caseloads.sortedBy { it.id }.map { Caseload(it.id, it.name) }
  }

  override fun getUsersRoles(username: String): List<String> {
    val roles = manageUsersWebClient.get()
      .uri("/users/$username/roles")
      .header("Content-Type", "application/json")
      .retrieve()
      .bodyToMono(ROLES)
      // HACKED TO ALLOW 404 - A bug in auth means this will always return 404 for external users. Being fixed now then we can remove this.
      .coerce404ToEmptyOrThrow()
      .block()?.map { it.roleCode }

    if (roles != null) return roles

    logger.warn("Returning hardcoded role for user $username - this needs removing when external users are supported")
    return listOf("ROLE_CVL_REPORTS")
  }

  override fun getPrisonUsersCaseload(username: String): CaseloadResponse {
    val caseload = manageUsersWebClient.get()
      .uri("/prisonusers/$username/caseloads")
      .header("Content-Type", "application/json")
      .retrieve()
      .bodyToMono(CaseloadResponse::class.java)
      // HACKED TO ALLOW 404 - This needs changing in the library to allow nulls if not a nomis user
      .coerce404ToEmptyOrThrow()
      .block()

    if (caseload != null) return caseload

    logger.warn("Returning hardcoded caseload for user $username - this needs removing before going live")

    return CaseloadResponse(
      username = username,
      active = false,
      accountType = "",
      activeCaseload = null,
      caseloads = emptyList(),
    )
  }
}
