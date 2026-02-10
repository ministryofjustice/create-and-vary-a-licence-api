package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.coerce404ToEmptyOrThrow
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

private const val HDC_BATCH_SIZE = 500
private const val COURT_OUTCOME_BATCH_SIZE = 500
private const val SENTENCE_AND_RECALL_BATCH_SIZE = 500

@Service
class PrisonApiClient(@param:Qualifier("oauthPrisonClient") val prisonerApiWebClient: WebClient) {

  companion object {
    private val log = LoggerFactory.getLogger(PrisonApiClient::class.java)
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

  fun getCurrentHdcStatuses(bookingIds: List<Long>, batchSize: Int = HDC_BATCH_SIZE) = batchRequests(batchSize, bookingIds) { batch ->
    prisonerApiWebClient
      .post()
      .uri("/licence/hdc/current-hdc-statuses")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(batch)
      .retrieve()
      .bodyToMono(typeReference<List<CurrentPrisonerHdcStatus>>())
      .block()
  }

  fun getCourtEventOutcomes(
    bookingIds: List<Long>,
    outcomeReasonCodes: List<String>,
    batchSize: Int = COURT_OUTCOME_BATCH_SIZE,
  ): List<CourtEventOutcome> = batchRequests(batchSize, bookingIds) { batch ->
    log.info("Fetching court event outcomes for ${batch.size} bookings with outcome codes $outcomeReasonCodes via Prison API")
    prisonerApiWebClient
      .post()
      .uri("/bookings/court-event-outcomes?outcomeReasonCodes=${outcomeReasonCodes.joinToString(",")}")
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

  fun getSentenceAndRecallTypes(bookingIds: List<Long>, batchSize: Int = SENTENCE_AND_RECALL_BATCH_SIZE) = batchRequests(batchSize, bookingIds) { batch ->
    prisonerApiWebClient
      .post()
      .uri("/offender-sentences/bookings/sentence-and-recall-types")
      .bodyValue(batch)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(typeReference<List<BookingSentenceAndRecallTypes>>())
      .block()
  }
}
