package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.PrisonerSearchByBookingIdsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.PrisonerSearchByPrisonerNumbersRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.ReleaseDateSearch
import java.time.LocalDate

@Service
class PrisonerSearchApiClient(@Qualifier("oauthPrisonerSearchClient") val prisonerSearchApiWebClient: WebClient) {

  fun searchPrisonersByBookingIds(bookingIds: Collection<Long>): List<PrisonerSearchPrisoner> {
    if (bookingIds.isEmpty()) return emptyList()
    return prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/booking-ids")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonerSearchByBookingIdsRequest(bookingIds.toList()))
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSearchPrisoner>>())
      .block() ?: emptyList()
  }

  fun searchPrisonersByNomisIds(nomisIds: List<String>): List<PrisonerSearchPrisoner> {
    if (nomisIds.isEmpty()) return emptyList()
    return prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/prisoner-numbers")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonerSearchByPrisonerNumbersRequest(nomisIds))
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSearchPrisoner>>())
      .block() ?: emptyList()
  }

  fun searchPrisonersByReleaseDate(earliestReleaseDate: LocalDate, latestReleaseDate: LocalDate, prisonIds: Set<String>, page: Int = 0): Page<PrisonerSearchPrisoner> {
    if (prisonIds.isEmpty()) return Page.empty()
    return prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/release-date-by-prison?size=2000&page=$page")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(ReleaseDateSearch(earliestReleaseDate = earliestReleaseDate, latestReleaseDate = latestReleaseDate, prisonIds = prisonIds))
      .retrieve()
      .bodyToMono(typeReference<PageResponse<PrisonerSearchPrisoner>>())
      .block()?.toPage() ?: Page.empty()
  }
}
