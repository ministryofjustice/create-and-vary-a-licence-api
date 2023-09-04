package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.PrisonerSearchByBookingIdsRequest

@Service
class PrisonerSearchApiClient(@Qualifier("oauthPrisonerSearchClient") val prisonerSearchApiWebClient: WebClient) {

  fun searchPrisonersByBookingIds(bookingIds: List<Long>): List<PrisonerSearchPrisoner> {
    if (bookingIds.isEmpty()) return emptyList()
    return prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/booking-ids")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonerSearchByBookingIdsRequest(bookingIds))
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSearchPrisoner>>())
      .block() ?: emptyList()
  }
}
