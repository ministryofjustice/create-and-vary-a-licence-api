package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponse

@Service
class AddressSearchService(
  private val osPlacesApiClient: OsPlacesApiClient,
  private val mapper: OsPlacesMapperToAddressSearchResponse,
) {

  fun searchForAddressesByText(searchQuery: String): List<AddressSearchResponse> = osPlacesApiClient.searchForAddressesByText(searchQuery).map { mapper.map(it) }

  fun searchForAddressesByPostcode(postcode: String): List<AddressSearchResponse> {
    val cleanedPostcode = postcode.uppercase().replace(Regex("[^A-Z0-9]"), "")
    return osPlacesApiClient.searchForAddressesByPostcode(cleanedPostcode).map { mapper.map(it) }
  }

  @Transactional
  fun searchForAddressByReference(reference: String): AddressSearchResponse = mapper.map(osPlacesApiClient.searchForAddressByReference(reference))
}
