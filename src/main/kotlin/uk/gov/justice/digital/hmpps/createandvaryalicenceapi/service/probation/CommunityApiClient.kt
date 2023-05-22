package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CommunityApiClient(@Qualifier("oauthCommunityApiClient") val communityApiClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getTeamsCodesForUser(staffIdentifier: Long): List<String> {
    val communityApiResponse = communityApiClient
      .get()
      .uri("/secure/staff/staffIdentifier/$staffIdentifier")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(User::class.java)
      .block()
    return communityApiResponse?.teams?.map { it.code }
      ?: throw IllegalStateException("Could not find user with staff identifier $staffIdentifier")
  }
}
