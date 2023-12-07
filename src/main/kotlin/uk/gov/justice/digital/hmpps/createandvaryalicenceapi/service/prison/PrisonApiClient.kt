package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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

  fun getHdcStatus(bookingId: Long): Mono<PrisonerHdcStatus> {
    return prisonerApiWebClient
      .get()
      .uri("/offender-sentences/booking/$bookingId/home-detention-curfews/latest")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(PrisonerHdcStatus::class.java)
      .onErrorResume { coerce404ResponseToNull(it) }
  }

  fun getHdcStatuses(bookingIds: List<Long>): List<PrisonerHdcStatus> {
    if (bookingIds.isEmpty()) return emptyList()
    return prisonerApiWebClient
      .post()
      .uri("/offender-sentences/home-detention-curfews/latest")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(bookingIds)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerHdcStatus>>())
      .block() ?: emptyList()
  }

  fun getCourtEventOutcomes(bookingIds: List<Long>): List<CourtEventOutcome> {
    return prisonerApiWebClient
      .post()
      .uri("/bookings/court-event-outcomes")
      .bodyValue(bookingIds)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<CourtEventOutcome>>())
      .block() ?: emptyList()
  }

  fun getPrisonInformation(prisonId: String): Prison {
    val prisonerApiResponse = prisonerApiWebClient
      .get()
      .uri("/agencies/prison/$prisonId")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(Prison::class.java)
      .block()

    return prisonerApiResponse
      ?: error("Unexpected null response from Prison API")
  }

  private fun <API_RESPONSE_BODY_TYPE> coerce404ResponseToNull(exception: Throwable): Mono<API_RESPONSE_BODY_TYPE> =
    with(exception) {
      when {
        this is WebClientResponseException && statusCode == NOT_FOUND -> {
          log.info("No resource found when calling prisoner-api ${request?.uri?.path}")
          Mono.empty()
        }

        else -> Mono.error(exception)
      }
    }
}
