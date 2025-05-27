package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.SearchQueryRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

@Component
class DeliusApiClient(@Qualifier("oauthDeliusApiClient") val deliusApiWebClient: WebClient) {
  companion object {
    private const val STAFF_EMAIL_BATCH = 500
    private const val STAFF_USERNAME_BATCH = 500
    private const val PROBATION_CASE_BATCH_SIZE = 500
    const val CASELOAD_PAGE_SIZE = 2000
  }

  fun getProbationCase(crnOrNomisId: String): ProbationCase = deliusApiWebClient
    .get()
    .uri("/probation-case/{crnOrNomisId}", crnOrNomisId)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(ProbationCase::class.java)
    .block() ?: error("Unexpected null response from API")

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

  fun getStaffByIdentifier(staffIdentifier: Long): User? = deliusApiWebClient
    .get()
    .uri("/staff/byid/{staffIdentifier}", staffIdentifier)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(User::class.java)
    .block()

  fun getOffenderManager(crnOrNomisId: String): CommunityManager? = deliusApiWebClient
    .get()
    .uri("/probation-case/{crnOrNomisId}/responsible-community-manager", crnOrNomisId)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(typeReference<CommunityManager>())
    .onErrorResume {
      when {
        it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
          Mono.empty()
        }

        else -> Mono.error(it)
      }
    }
    .block() ?: error("Unexpected null response from API")

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

  fun getStaffDetailsByUsername(usernames: List<String>): List<User> = batchRequests(STAFF_USERNAME_BATCH, usernames) { batch ->
    deliusApiWebClient
      .post()
      .uri("/staff")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(batch)
      .retrieve()
      .bodyToMono(typeReference<List<User>>())
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
}
