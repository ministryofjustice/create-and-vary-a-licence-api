package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CommunityApiClient(@Qualifier("oauthCommunityApiClient") val communityApiClient: WebClient) {

  fun getTeamsCodesForUser(staffIdentifier: Long): List<String> {
    val user = communityApiClient
      .get()
      .uri("/secure/staff/staffIdentifier/$staffIdentifier")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(User::class.java)
      .block()

    val userTeams = user?.teams

    if (userTeams != null) {
      return userTeams.map { team: Team -> team.code }
    } else {
      return emptyList()
    }
  }
}
