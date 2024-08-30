package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class HdcApiClient(@Qualifier("oauthHdcApiClient") val hdcApiWebClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getByBookingId(bookingId: Long): HdcLicenceData? {
    return hdcApiWebClient
      .get()
      .uri("/licence/hdc/$bookingId")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(HdcLicenceData::class.java)
      .onErrorResume { propagate404Response(it, bookingId) }
      .block()
  }

  private fun <API_RESPONSE_BODY_TYPE> propagate404Response(exception: Throwable, bookingId: Long): Mono<API_RESPONSE_BODY_TYPE> =
    with(exception) {
      when {
        this is WebClientResponseException && statusCode == NOT_FOUND -> {
          log.info("No resource found when calling hdc-api ${request?.uri?.path}")
          Mono.error(EntityNotFoundException("No licence data found for $bookingId"))
        }
        else -> Mono.error(exception)
      }
    }
}
