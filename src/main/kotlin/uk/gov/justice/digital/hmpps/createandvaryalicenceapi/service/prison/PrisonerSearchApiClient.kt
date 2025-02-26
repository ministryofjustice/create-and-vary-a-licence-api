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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.Batching.batchRequests
import java.time.LocalDate

@Service
class PrisonerSearchApiClient(@Qualifier("oauthPrisonerSearchClient") val prisonerSearchApiWebClient: WebClient) {

  companion object {
    private const val PAGE_SIZE = 2000
    private const val BY_PRISON_NUMBER_BATCH_SIZE = 1000
    private const val BY_BOOKING_ID_BATCH_SIZE = 1000
  }

  fun searchPrisonersByBookingIds(bookingIds: Collection<Long>) = batchRequests(BY_BOOKING_ID_BATCH_SIZE, bookingIds) { batch ->
    prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/booking-ids")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonerSearchByBookingIdsRequest(batch.toList()))
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSearchPrisoner>>())
      .block()
  }

  fun searchPrisonersByNomisIds(nomisIds: List<String>) = batchRequests(BY_PRISON_NUMBER_BATCH_SIZE, nomisIds) { batch ->
    prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/prisoner-numbers")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(PrisonerSearchByPrisonerNumbersRequest(batch))
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSearchPrisoner>>())
      .block()
  }

  fun searchPrisonersByReleaseDate(
    earliestReleaseDate: LocalDate,
    latestReleaseDate: LocalDate,
    prisonIds: Set<String>,
    page: Int = 0,
  ): Page<PrisonerSearchPrisoner> {
    if (prisonIds.isEmpty()) return Page.empty()
    val result = prisonerSearchApiWebClient
      .post()
      .uri("/prisoner-search/release-date-by-prison?size=$PAGE_SIZE&page=$page")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        ReleaseDateSearch(
          earliestReleaseDate = earliestReleaseDate,
          latestReleaseDate = latestReleaseDate,
          prisonIds = prisonIds,
        ),
      )
      .retrieve()
      .bodyToMono(typeReference<PageResponse<PrisonerSearchPrisoner>>())
      .block()?.toPage() ?: Page.empty()
    return result
  }

  fun getAllByReleaseDate(from: LocalDate, to: LocalDate, pageSize: Int = PAGE_SIZE): List<PrisonerSearchPrisoner> {
    val result = mutableListOf<PrisonerSearchPrisoner>()
    var pageNumber = 0

    while (pageNumber >= 0) {
      // eslint-disable-next-line no-await-in-loop
      val page = prisonerSearchApiWebClient
        .post()
        .uri("/prisoner-search/release-date-by-prison?size=$pageSize&page=$pageNumber")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(
          ReleaseDateSearch(
            earliestReleaseDate = from,
            latestReleaseDate = to,
            prisonIds = emptySet(),
          ),
        )
        .retrieve()
        .bodyToMono(typeReference<PageResponse<PrisonerSearchPrisoner>>())
        .block()?.toPage() ?: Page.empty()
      pageNumber = if (pageNumber < page.totalPages - 1) pageNumber + 1 else -1
      result.addAll(page.content)
    }
    return result
  }
}
