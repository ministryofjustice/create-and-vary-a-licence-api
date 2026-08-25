package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto.OsPlacesApiAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesDpaMapperToAddressSearchResponseMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesLpiMapperToAddressSearchResponseMapper

@Component
class AddressSearchPaginator(
  private val osPlacesApiClient: OsPlacesApiClient,
  private val addressDpaMapper: OsPlacesDpaMapperToAddressSearchResponseMapper,
  private val addressLpiMapper: OsPlacesLpiMapperToAddressSearchResponseMapper,
  @param:Value("\${address.os-places.search.page-size:100}") private val pageSize: Int,
  @param:Value("\${address.search.max-total:200}") private val maxTotal: Int,
) {

  fun searchByText(searchQuery: String): List<AddressSearchResponse> = paginate(
    fetchCallBack = { _, pageable ->
      osPlacesApiClient.searchForAddressesByText(pageable, searchQuery)
    },
    mapResults = { wrapper ->
      wrapper.dpa?.let(addressDpaMapper::map)
        ?: wrapper.lpi?.let(addressLpiMapper::map)
    },
  )

  private fun paginate(
    fetchCallBack: (page: Int, pageable: PageRequest) -> List<OsPlacesApiAddress>,
    mapResults: (OsPlacesApiAddress) -> AddressSearchResponse?,
  ): List<AddressSearchResponse> {
    val totalResults = mutableListOf<OsPlacesApiAddress>()
    var pageCount = 0

    while (totalResults.size < maxTotal) {
      val resultsFromApiCall = fetchCallBack(pageCount, PageRequest.of(pageCount, pageSize))

      if (resultsFromApiCall.isNotEmpty()) {
        totalResults.addAll(resultsFromApiCall)
      }

      if (resultsFromApiCall.size < pageSize) break

      pageCount++
    }

    cleanUpAddresses(totalResults)

    return totalResults.take(maxTotal).mapNotNull { mapResults(it) }
  }

  private fun cleanUpAddresses(totalResults: MutableList<OsPlacesApiAddress>) {
    // Remove historical addresses, otherwise we will get duplicate addresses
    totalResults.removeIf { it.lpi?.lpiLogicalStatusCodeDescription.equals("HISTORICAL", ignoreCase = true) }

    val listOfDpaUprn = totalResults.mapNotNull { it.dpa?.uprn }.toSet()
    totalResults.forEach {
      if (listOfDpaUprn.contains(it.lpi?.uprn)) {
        // removed duplicate lpi address that exists as a dpa address
        it.lpi = null
      }
    }
  }
}
