package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Component
class CommunityApiClient(@Qualifier("oauthCommunityApiClient") val communityApiClient: WebClient) {

  fun getTeamsCodesForUser(staffIdentifier: Long): List<String> {
    val communityApiResponse = communityApiClient
      .get()
      .uri("/secure/staff/staffIdentifier/$staffIdentifier")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(User::class.java)
      .block()
    return communityApiResponse?.teams?.map { it.code }
      ?: error("Unexpected null response from API")
  }

  fun getAllOffenderManagers(crn: String): List<CommunityOrPrisonOffenderManager> {
    val communityApiResponse = communityApiClient
      .get()
      .uri("/secure/offenders/crn/$crn/allOffenderManagers")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<CommunityOrPrisonOffenderManager>>())
      .block()
    return communityApiResponse
      ?: error("Unexpected null response from API")
  }
}
