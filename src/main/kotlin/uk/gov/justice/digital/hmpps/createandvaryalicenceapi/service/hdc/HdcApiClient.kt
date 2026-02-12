package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurrentPrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ResponseUtils.propagateAny404
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests

private const val HDC_BATCH_SIZE = 500

@Service
class HdcApiClient(@param:Qualifier("oauthHdcApiClient") val hdcApiWebClient: WebClient) {

  fun getByBookingId(bookingId: Long): HdcLicenceData = hdcApiWebClient
    .get()
    .uri("/licence/hdc/$bookingId")
    .accept(MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToMono(HdcLicenceData::class.java)
    .propagateAny404 { "No licence data found for $bookingId" }
    .block()!!

  fun getCurrentHdcStatuses(bookingIds: List<Long>, batchSize: Int = HDC_BATCH_SIZE) = batchRequests(batchSize, bookingIds) { batch ->
    hdcApiWebClient
      .post()
      .uri("/licence/hdc/current-hdc-statuses")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(batch)
      .retrieve()
      .bodyToMono(typeReference<List<CurrentPrisonerHdcStatus>>())
      .block()
  }
}
