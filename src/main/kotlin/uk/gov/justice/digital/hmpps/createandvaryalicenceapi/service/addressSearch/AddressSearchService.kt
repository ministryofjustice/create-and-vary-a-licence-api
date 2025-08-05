package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponseMapper

@Service
class AddressSearchService(
  private val osPlacesApiClient: OsPlacesApiClient,
  private val mapper: OsPlacesMapperToAddressSearchResponseMapper,
  private val addressSearchPaginator: AddressSearchPaginator,
) {

  fun searchForAddressesByText(searchQuery: String): List<AddressSearchResponse> {
    if (searchQuery.length < 3) {
      // Prevent silly queries
      return listOf()
    }
    return addressSearchPaginator.searchByText(searchQuery)
  }

  @Transactional
  fun searchForAddressByReference(reference: String): AddressSearchResponse = mapper.map(osPlacesApiClient.searchForAddressByReference(reference))
}
