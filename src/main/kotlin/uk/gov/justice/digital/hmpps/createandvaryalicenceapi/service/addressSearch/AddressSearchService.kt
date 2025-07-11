package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponseMapper

@Service
class AddressSearchService(
  private val osPlacesApiClient: OsPlacesApiClient,
  private val mapper: OsPlacesMapperToAddressSearchResponseMapper,
) {

  fun searchForAddressesByText(searchQuery: String, page: Int, pageSize: Int): List<AddressSearchResponse> {
    if (searchQuery.length < 3) {
      // Prevent silly queries
      return listOf()
    }
    val pageable = PageRequest.of(page, pageSize)
    return osPlacesApiClient.searchForAddressesByText(pageable, searchQuery).map { mapper.map(it) }
  }

  fun searchForAddressesByPostcode(postcode: String, page: Int, pageSize: Int): List<AddressSearchResponse> {
    val pageable = PageRequest.of(page, pageSize)
    val cleanedPostcode = postcode.uppercase().replace(Regex("[^A-Z0-9]"), "")
    return osPlacesApiClient.searchForAddressesByPostcode(pageable, cleanedPostcode).map { mapper.map(it) }
  }

  @Transactional
  fun searchForAddressByReference(reference: String): AddressSearchResponse = mapper.map(osPlacesApiClient.searchForAddressByReference(reference))
}
