package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto.OsCommonAddress
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
      flattenToList(osPlacesApiClient.searchForAddressesByText(pageable, searchQuery))
    },
    mapResults = { address ->
      if (address.isDeliveryPointAddress()) {
        addressDpaMapper.map(address)
      } else {
        addressLpiMapper.map(address)
      }
    },
  )

  private fun paginate(
    fetchCallBack: (page: Int, pageable: PageRequest) -> List<OsCommonAddress>,
    mapResults: (OsCommonAddress) -> AddressSearchResponse?,
  ): List<AddressSearchResponse> {
    val totalResults = mutableListOf<OsCommonAddress>()
    var pageCount = 0
    val listOfDpaUprn = mutableSetOf<String>()

    while (totalResults.size < maxTotal) {
      val resultsFromApiCall = fetchCallBack(pageCount, PageRequest.of(pageCount, pageSize))

      if (resultsFromApiCall.isNotEmpty()) {
        add(resultsFromApiCall, listOfDpaUprn, totalResults)
      }

      if (resultsFromApiCall.size < pageSize) break

      pageCount++
    }

    return totalResults.mapNotNull { mapResults(it) }
  }

  private fun add(
    resultsFromApiCall: List<OsCommonAddress>,
    listOfDpaUprn: MutableSet<String>,
    totalResults: MutableList<OsCommonAddress>,
  ) {
    resultsFromApiCall.forEach { address ->
      if (address.isDeliveryPointAddress()) {
        listOfDpaUprn.add(address.uprn)
      }
    }
    totalResults.addAll(resultsFromApiCall)
    totalResults.removeIf { !it.isDeliveryPointAddress() && listOfDpaUprn.contains(it.uprn) }
  }

  private fun flattenToList(addresses: List<OsPlacesApiAddress>): List<OsCommonAddress> {
    val addressList = mutableListOf<OsCommonAddress>()
    addresses.forEach { results ->
      results.dpa?.let { addressList.add(it) }
      results.lpi?.let {
        if (!it.lpiLogicalStatusCodeDescription.equals("HISTORICAL", ignoreCase = true)) {
          addressList.add(it)
        }
      }
    }
    return addressList
  }
}
