package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.OsPlacesApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponseMapper

@Component
class AddressSearchPaginator(
  private val osPlacesApiClient: OsPlacesApiClient,
  private val addressMapper: OsPlacesMapperToAddressSearchResponseMapper,
  @Value("\${address.os-places.search.page-size:100}") private val pageSize: Int,
  @Value("\${address.search.max-total:500}") private val maxTotal: Int,
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
    val results = mutableListOf<R>()
    var page = 0
    while (results.size < maxTotal) {
      val pageable = PageRequest.of(page, pageSize)
      val rawPage = fetchCallBack(page, pageable)
      if (rawPage.isEmpty()) break

      results += rawPage.map(mapResults)

      if (rawPage.size < pageSize) break
      page++
    }
    return results.take(maxTotal)
  }
}
