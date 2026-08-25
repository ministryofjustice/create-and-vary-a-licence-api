package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto.OsCommonAddress

private const val ADDRESS_ITEM_SEPARATOR = ", "
private val KNOWN_COUNTRIES = listOf("England", "Scotland", "Wales", "Northern Ireland", "Ireland")

@Component
class OsPlacesLpiMapperToAddressSearchResponseMapper {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun map(address: OsCommonAddress): AddressSearchResponse {
    log.info("Mapping LPI address to AddressSearchResponse: {}", address)

    val (firstLine, secondLine) = mapAddressLines(address)

    return AddressSearchResponse(
      uprn = address.uprn,
      firstLine = firstLine,
      secondLine = secondLine,
      townOrCity = address.postTown.orEmpty(),
      county = address.county.orEmpty(),
      postcode = address.postcode,
      country = getCountry(address),
    )
  }

  private fun mapAddressLines(address: OsCommonAddress): Pair<String, String> {
    var addressItems = address.address
      .split(",")
      .map(String::trim)
      .filter(String::isNotEmpty)

    addressItems = removeIfAtEnd(addressItems, address.postcode)
    addressItems = removeIfAtEnd(addressItems, address.county)
    addressItems = removeIfAtEnd(addressItems, address.postTown)

    val firstElementIsANumber = addressItems.firstOrNull()?.matches(Regex("\\d+")) == true

    val firstLine: String
    val secondLine: String

    if (firstElementIsANumber && addressItems.size > 1) {
      firstLine = "${addressItems[0]} ${addressItems[1]}"
      secondLine = addressItems.drop(2).joinToString(ADDRESS_ITEM_SEPARATOR)
    } else {
      firstLine = addressItems.firstOrNull().orEmpty()
      secondLine = addressItems.drop(1).joinToString(ADDRESS_ITEM_SEPARATOR)
    }

    return firstLine to secondLine
  }

  private fun removeIfAtEnd(
    addressItems: List<String>,
    item: String?,
  ): List<String> = if (item != null && addressItems.lastOrNull().equals(item, ignoreCase = true)) {
    addressItems.dropLast(1)
  } else {
    addressItems
  }

  private fun getCountry(address: OsCommonAddress): String {
    val description = address.countryDescription.trim()

    return KNOWN_COUNTRIES.firstOrNull {
      description.contains(it, ignoreCase = true)
    } ?: ""
  }
}
