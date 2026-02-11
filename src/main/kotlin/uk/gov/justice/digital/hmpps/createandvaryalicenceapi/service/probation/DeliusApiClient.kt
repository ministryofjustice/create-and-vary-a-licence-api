package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.SearchQueryRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.StaffNameResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.UserAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.coerce404ToEmptyOrThrow
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

@Component
class DeliusApiClient(@param:Qualifier("oauthDeliusApiClient") val deliusApiWebClient: WebClient) {
  companion object {
    private const val STAFF_EMAIL_BATCH = 500
    private const val STAFF_USERNAME_BATCH = 500
    private const val PROBATION_CASE_BATCH_SIZE = 500
    private const val CHECK_ACCESS_BATCH_SIZE = 500
    const val CASELOAD_PAGE_SIZE = 2000
  }

  fun getProbationCase(crnOrNomisId: String): ProbationCase? = deliusApiWebClient
    .get()
    .uri("/probation-case/{crnOrNomisId}", crnOrNomisId)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<ProbationCase>()
    .coerce404ToEmptyOrThrow()
    .block()

  fun getProbationCases(
    crnsOrNomisIds: List<String>,
    batchSize: Int = PROBATION_CASE_BATCH_SIZE,
  ): List<ProbationCase> = batchRequests(batchSize, crnsOrNomisIds) { batch ->
    deliusApiWebClient
      .post()
      .uri("/probation-case")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<ProbationCase>>())
      .block()
  }

  fun getTeamsCodesForUser(staffIdentifier: Long): List<String> {
    val communityApiResponse = getStaffByIdentifier(staffIdentifier)
    return communityApiResponse?.teams?.map { it.code }
      ?: error("Unexpected null response from API")
  }

  fun getStaffByCode(staffCode: String): User? = deliusApiWebClient
    .get()
    .uri("/staff/bycode/{code}", staffCode)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<User>()
    .coerce404ToEmptyOrThrow()
    .block()

  fun getStaffByIdentifier(staffIdentifier: Long): User? = deliusApiWebClient
    .get()
    .uri("/staff/byid/{staffIdentifier}", staffIdentifier)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono<User>()
    .coerce404ToEmptyOrThrow()
    .block()

  fun getOffenderManager(crnOrNomisId: String): CommunityManager? = deliusApiWebClient
    .get()
    .uri("/probation-case/{crnOrNomisId}/responsible-community-manager", crnOrNomisId)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(typeReference<CommunityManager>())
    .coerce404ToEmptyOrThrow()
    .block()

  fun getOffenderManagers(
    crnsOrNomisIds: List<String>,
    batchSize: Int = PROBATION_CASE_BATCH_SIZE,
  ): List<CommunityManager> = batchRequests(batchSize, crnsOrNomisIds) { batch ->
    deliusApiWebClient
      .post()
      .uri("/probation-case/responsible-community-manager")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<CommunityManager>>())
      .block()
  }

  fun getOffenderManagersWithoutUser(
    crnsOrNomisIds: List<String>,
    batchSize: Int = PROBATION_CASE_BATCH_SIZE,
  ): List<CommunityManagerWithoutUser> = batchRequests(batchSize, crnsOrNomisIds) { batch ->
    deliusApiWebClient
      .post()
      .uri("/probation-case/responsible-community-manager?includeEmail=false")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<CommunityManagerWithoutUser>>())
      .block()
  }

  fun getStaffEmails(crns: List<String>) = batchRequests(STAFF_EMAIL_BATCH, crns) { batch ->
    deliusApiWebClient
      .post()
      .uri("/probation-case/responsible-community-manager")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<StaffEmail>>())
      .block()
  }

  fun getStaffDetailsByUsername(usernames: List<String>): List<StaffNameResponse> = batchRequests(STAFF_USERNAME_BATCH, usernames) { batch ->
    deliusApiWebClient
      .post()
      .uri("/staff")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(batch)
      .retrieve()
      .bodyToMono(typeReference<List<StaffNameResponse>>())
      .block()
  }

  fun getManagedOffenders(staffIdentifier: Long): List<ManagedOffenderCrn> = deliusApiWebClient
    .get()
    .uri("/staff/byid/{staffIdentifier}/caseload/managed-offenders", staffIdentifier)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(typeReference<List<ManagedOffenderCrn>>())
    .block() ?: error("Unexpected null response from Delius staff caseload")

  fun getManagedOffendersByTeam(teamCode: String): List<ManagedOffenderCrn> = deliusApiWebClient
    .get()
    .uri("/team/$teamCode/caseload/managed-offenders")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(typeReference<List<ManagedOffenderCrn>>())
    .block() ?: error("Unexpected null response from Delius staff caseload")

  fun getTeamManagedOffenders(
    staffIdentifier: Long,
    query: String,
    page: PageRequest = PageRequest.of(0, CASELOAD_PAGE_SIZE),
  ): CaseloadResponse = deliusApiWebClient
    .post()
    .uri {
      it.path("/staff/byid/{staffIdentifier}/caseload/team-managed-offenders")
        .apply {
          with(page) {
            queryParam("page", pageNumber)
            queryParam("size", pageSize)
            sort.forEach { order -> queryParam("sort", "${order.property},${order.direction.name}") }
          }
        }
        .build(staffIdentifier)
    }
    .bodyValue(SearchQueryRequest(query))
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(typeReference<CaseloadResponse>())
    .block() ?: error("Unexpected null response from Delius staff caseload")

  fun assignDeliusRole(username: String): ResponseEntity<Void> = deliusApiWebClient.put()
    .uri("/users/$username/roles")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .toBodilessEntity()
    .block() ?: error("Unexpected response while assigning delius role for user: $username")

  fun getCheckUserAccess(
    username: String,
    crns: List<String>,
    batchSize: Int = CHECK_ACCESS_BATCH_SIZE,
  ): List<UserAccessResponse> = batchRequests(batchSize, crns) { batch ->
    val response = deliusApiWebClientdeliusApiWebClient
      .post()
      .uri("/users/$username/access")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<UserAccessResponse>())
      .block() ?: error("Unexpected null response from Delius check user access for user: $username")
    response.access
  }
}
