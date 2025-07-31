package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponseMapper

@Component
class AddressSearchPaginator(
  private val osPlacesApiClient: OsPlacesApiClient,
  private val addressMapper: OsPlacesMapperToAddressSearchResponseMapper,
  @Value("\${address.os-places.search.page-size:100}") private val pageSize: Int,
  @Value("\${address.search.max-total:200}") private val maxTotal: Int,
) {

  fun searchByText(searchQuery: String): List<AddressSearchResponse> = paginate(
    fetchCallBack = { page, pageable ->
      osPlacesApiClient.searchForAddressesByText(pageable, searchQuery)
    },
    mapResults = { addressMapper.map(it) },
  )

  fun searchByPostcode(postcode: String): List<AddressSearchResponse> = paginate(
    fetchCallBack = { page, pageable ->
      osPlacesApiClient.searchForAddressesByPostcode(pageable, postcode)
    },
    mapResults = { addressMapper.map(it) },
  )

  private fun <T, R> paginate(
    fetchCallBack: (page: Int, pageable: PageRequest) -> List<T>,
    mapResults: (T) -> R,
  ): List<R> {
    val totalResults = mutableListOf<T>()
    var pageCount = 0
    while (totalResults.size < maxTotal) {
      val resultsFromApiCall = fetchCallBack(pageCount, PageRequest.of(pageCount, pageSize))
      if (resultsFromApiCall.isNotEmpty()) {
        totalResults.addAll(resultsFromApiCall)
      }
      if (resultsFromApiCall.size < pageSize) break
      pageCount++
    }
    return totalResults.take(maxTotal).map { mapResults(it) }
  }
}
