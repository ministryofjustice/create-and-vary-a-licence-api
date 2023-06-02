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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference

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

  fun hdcStatuses(bookingIds: List<Long>): List<PrisonerHdcStatus>{
    return prisonerApiWebClient
      .post()
      .uri("/offender-sentences/home-detention-curfews/latest")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(bookingIds)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerHdcStatus>>())
      .onErrorResume { webClientErrorHandler(it) }
      .block() ?: emptyList<PrisonerHdcStatus>()
  }

  fun offenceHistories(bookingIds: List<Long>): List<PrisonerOffenceHistory>{
    return prisonerApiWebClient
      .post()
      .uri("/bookings/offence-history")
      .bodyValue(bookingIds)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerOffenceHistory>>())
      .onErrorResume { webClientErrorHandler(it) }
      .block() ?: emptyList<PrisonerOffenceHistory>()
  }

  fun getIS91AndExtraditionBookingIds(bookingIds: List<Long>): List<Long>{
    val allOffenceHistories = offenceHistories(bookingIds)
    val desiredResultCodes: Array<String> = arrayOf("5500", "4022", "3006", "5502")
    val is91AndExtraditionOffenceHistories = allOffenceHistories.filter {
      desiredResultCodes.contains(it.primaryResultCode) ||
      desiredResultCodes.contains(it.secondaryResultCode) ||
      it.offenceCode == "IA99000-001N"
    }
    return is91AndExtraditionOffenceHistories.map { it.bookingId }
  }

  private fun <API_RESPONSE_BODY_TYPE> webClientErrorHandler(exception: Throwable): Mono<API_RESPONSE_BODY_TYPE> =
    with(exception) {
      if (this is WebClientResponseException) {
        val uriPath = request?.uri?.path
        when (statusCode) {
          FORBIDDEN -> {
            log.error("Client token does not have correct role to call prisoner-api $uriPath")
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
