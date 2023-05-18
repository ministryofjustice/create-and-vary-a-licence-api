package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class CommunityApiClient(@Qualifier("oauthCommunityApiClient") val communityApiClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getTeamsCodesForUser(staffIdentifier: Long): List<String> {
    try {
      val user = communityApiClient
        .get()
        .uri("/secure/staff/staffIdentifier/$staffIdentifier")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(User::class.java)
        .block()
      if (user === null) {
        return emptyList()
      } else {
        val userTeams = user.teams
        return userTeams.map { team: Team -> team.code }
      }
    } catch (exception: WebClientResponseException) {
      with(exception) {
        val uriPath = request?.uri?.path
        log.error("No user found for staff when calling the community-api $uriPath with the following message ${exception.message}")
      }
      return emptyList()
    }
  }
}
