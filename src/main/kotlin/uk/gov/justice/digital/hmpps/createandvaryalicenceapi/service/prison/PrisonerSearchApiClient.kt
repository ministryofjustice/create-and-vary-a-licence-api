package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

@Service
class PrisonerSearchApiClient(@Qualifier("oauthPrisonerSearchClient") val prisonerSearchApiWebClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun searchPrisonersByBookingIds(bookingIds: List<Long>): List<PrisonerSearchPrisoner> {
    return prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/booking-ids")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(bookingIds)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSearchPrisoner>>())
      .onErrorResume { webClientErrorHandler(it) }
      .block() ?: emptyList<PrisonerSearchPrisoner>()
  }

  private fun <API_RESPONSE_BODY_TYPE> webClientErrorHandler(exception: Throwable): Mono<API_RESPONSE_BODY_TYPE> =
    with(exception) {
      if (this is WebClientResponseException) {
        val uriPath = request?.uri?.path
        when (statusCode) {
          HttpStatus.FORBIDDEN -> {
            log.error("Client token does not have correct role to call prisoner-search-api $uriPath")
          }
          HttpStatus.NOT_FOUND -> {
            log.info("No resource found when calling prisoner-search-api $uriPath")
          }
          else -> {
            log.error("Failed to call prisoner-search-api $uriPath [statusCode: $statusCode, body: ${this.responseBodyAsString}]")
          }
        }
      } else {
        log.error("Failed to call prisoner-search-api", exception)
      }
    }.let { Mono.empty() }
}
