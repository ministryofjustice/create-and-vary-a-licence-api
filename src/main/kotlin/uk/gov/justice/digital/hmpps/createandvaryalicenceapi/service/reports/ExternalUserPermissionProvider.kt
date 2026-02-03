package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.NoDataAvailableException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.CaseloadResponse
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.security.UserPermissionProvider
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model.Caseload

const val WARNING_NO_NOMIS_USER_INFO = "No access to NOMIS user data."

@Component
class ExternalUserPermissionProvider(@param:Qualifier("oauthManageUsersWebClient") private val manageUsersWebClient: WebClient) : UserPermissionProvider {
  init {
    log.info("ExternalUserPermissionProvider initialized")
  }

  companion object {
    data class RolesResponse(val roleCode: String)

    val log = LoggerFactory.getLogger(ExternalUserPermissionProvider::class.java)
  }

  override fun getActiveCaseloadId(username: String): String = throw NoDataAvailableException(WARNING_NO_NOMIS_USER_INFO)

  override fun getCaseloads(username: String): List<Caseload> = emptyList()

  override fun getUsersRoles(username: String) = manageUsersWebClient.get()
    .uri("/users/$username/roles")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(typeReference<List<RolesResponse>>())
    .block()?.map { it.roleCode }!!

  override fun getPrisonUsersCaseload(username: String): CaseloadResponse = throw NoDataAvailableException(WARNING_NO_NOMIS_USER_INFO)
}
