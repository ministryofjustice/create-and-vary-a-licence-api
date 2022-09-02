package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class PrisonApiClient(@Qualifier("oauthPrisonClient") val prisonerApiWebClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun hdcStatus(bookingId: Long): Mono<PrisonerHdcStatus> {
    return prisonerApiWebClient
      .get()
      .uri("/offender-sentences/booking/$bookingId/home-detention-curfews/latest")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(PrisonerHdcStatus::class.java)
      .onErrorResume { webClientErrorHandler(it) }
  }

  private fun <API_RESPONSE_BODY_TYPE> webClientErrorHandler(exception: Throwable): Mono<API_RESPONSE_BODY_TYPE> =
    with(exception) {
      if (this is WebClientResponseException) {
        val uriPath = request?.uri?.path
        when (statusCode) {
          FORBIDDEN -> {
            log.info("Client token does not have correct role to call prisoner-api $uriPath")
          }
          NOT_FOUND -> {
            log.info("No resource found when calling prisoner-api $uriPath")
          }
          else -> {
            log.error("Failed to call prisoner-api $uriPath [statusCode: $statusCode, body: ${this.responseBodyAsString}]")
          }
        }
      } else {
        log.error("Failed to call prisoner-api", exception)
      }
    }.let { Mono.empty() }
}
