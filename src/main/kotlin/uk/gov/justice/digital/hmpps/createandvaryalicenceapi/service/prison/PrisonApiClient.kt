package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.coerce404ToEmptyOrThrow
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

@Service
class PrisonApiClient(@param:Qualifier("oauthPrisonClient") val prisonerApiWebClient: WebClient) {

  companion object {
    private const val HDC_BATCH_SIZE = 500
    private const val COURT_OUTCOME_BATCH_SIZE = 500
  }

  fun getHdcStatus(bookingId: Long): PrisonerHdcStatus = prisonerApiWebClient
    .get()
    .uri("/offender-sentences/booking/{bookingId}/home-detention-curfews/latest", bookingId)
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(PrisonerHdcStatus::class.java)
    .coerce404ToEmptyOrThrow()
    .block()
    ?: PrisonerHdcStatus(passed = false, approvalStatus = "UNKNOWN")

  fun getHdcStatuses(bookingIds: List<Long>, batchSize: Int = HDC_BATCH_SIZE) = batchRequests(batchSize, bookingIds) { batch ->
    prisonerApiWebClient
      .post()
      .uri("/offender-sentences/home-detention-curfews/latest")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(batch)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerHdcStatus>>())
      .block()
  }

  fun getCourtEventOutcomes(bookingIds: List<Long>, batchSize: Int = COURT_OUTCOME_BATCH_SIZE) = batchRequests(batchSize, bookingIds) { batch ->
    prisonerApiWebClient
      .post()
      .uri("/bookings/court-event-outcomes")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<CourtEventOutcome>>())
      .block()
  }

  fun getPrisonInformation(prisonId: String): Prison {
    val prisonerApiResponse = prisonerApiWebClient
      .get()
      .uri("/agencies/prison/{prisonId}", prisonId)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(Prison::class.java)
      .block()

    return prisonerApiResponse
      ?: error("Unexpected null response from Prison API")
  }

  fun getPrisonerDetail(nomsId: String): PrisonApiPrisoner {
    val prisonApiResponse = prisonerApiWebClient
      .get()
      .uri("/offenders/{nomsId}", nomsId)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(PrisonApiPrisoner::class.java)
      .block()

    return prisonApiResponse ?: error("Unexpected null response from Prison API for nomsId $nomsId")
  }
}
