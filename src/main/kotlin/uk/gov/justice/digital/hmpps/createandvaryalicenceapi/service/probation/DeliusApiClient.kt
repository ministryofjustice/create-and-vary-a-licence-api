package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Component
class DeliusApiClient(@Qualifier("oauthDeliusApiClient") val communityApiClient: WebClient) {
  companion object {
    private const val STAFF_EMAIL_BATCH = 500
  }

  fun getTeamsCodesForUser(staffIdentifier: Long): List<String> {
    val communityApiResponse = getStaffByIdentifier(staffIdentifier)
    return communityApiResponse?.teams?.map { it.code }
      ?: error("Unexpected null response from API")
  }

  fun getStaffByIdentifier(staffIdentifier: Long): User? =
    communityApiClient
      .get()
      .uri("/staff/byid/$staffIdentifier", staffIdentifier)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(User::class.java)
      .block()

  fun getOffenderManager(crn: String): CommunityOrPrisonOffenderManager? {
    val communityApiResponse = communityApiClient
      .get()
      .uri("/probation-case/{crn}/responsible-community-manager", crn)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<CommunityOrPrisonOffenderManager>())
      .onErrorResume {
        when {
          it is WebClientResponseException && it.statusCode == HttpStatus.NOT_FOUND -> {
            Mono.empty()
          }

          else -> Mono.error(it)
        }
      }
      .block()
    return communityApiResponse
      ?: error("Unexpected null response from API")
  }

  fun getStaffEmails(crns: List<String>): List<StaffEmail> {
    if (crns.isEmpty()) return emptyList()

    val batchedCrns = crns.chunked(STAFF_EMAIL_BATCH)
    val results = batchedCrns.map { batch ->
      communityApiClient
        .post()
        .uri("/probation-case/responsible-community-manager")
        .bodyValue(batch)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(typeReference<List<StaffEmail>>())
        .block() ?: error("Unexpected null response from API")
    }

    return results.flatten()
  }

  fun getStaffDetailsByUsername(usernames: List<String>): List<User> {
    val communityApiResponse = communityApiClient
      .post()
      .uri("/staff")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(usernames)
      .retrieve()
      .bodyToMono(typeReference<List<User>>())
      .block()
    return communityApiResponse
      ?: error("Unexpected null response from API")
  }

  fun getManagedOffenders(staffIdentifier: Long): List<ManagedOffenderCrn> {
    val communityApiResponse = communityApiClient
      .get()
      .uri("/staff/byid/$staffIdentifier/caseload/managed-offenders", staffIdentifier)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<ManagedOffenderCrn>>())
      .block()
    return communityApiResponse ?: error("Unexpected null response from community API get staff caseload")
  }

  fun getManagedOffendersByTeam(teamCode: String): List<ManagedOffenderCrn> {
    val communityApiResponse = communityApiClient
      .get()
      .uri("/team/$teamCode/caseload/managed-offenders")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<ManagedOffenderCrn>>())
      .block()
    return communityApiResponse ?: error("Unexpected null response from community API get staff caseload")
  }
}
