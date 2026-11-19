package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class RemandAndSentencingApiClient(@param:Qualifier("oauthRemandAndSentencingApiClient") val remandAndSentencingApiClient: WebClient) {
  fun getPersonStatuses(prisonNumbers: List<String>): PersonStatusesResponse? = remandAndSentencingApiClient
    .post()
    .uri("/person/statuses")
    .accept(MediaType.APPLICATION_JSON)
    .bodyValue(PersonStatusesRequest(prisonNumbers))
    .retrieve()
    .bodyToMono<PersonStatusesResponse>()
    .block()
}
