package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.DeliveryPointAddress

private const val ADDRESS_ITEM_SEPARATOR = ", "

@Component
class OsPlacesMapperToAddressSearchResponseMapper {

  fun map(deliveryPointAddress: DeliveryPointAddress): AddressSearchResponse {
    val firstLineDetails = getAddressFirstLine(deliveryPointAddress)

    return AddressSearchResponse(
      reference = deliveryPointAddress.uprn,
      firstLine = firstLineDetails.first,
      secondLine = deliveryPointAddress.locality?.let { getSecondLine(firstLineDetails.second, it) },
      townOrCity = deliveryPointAddress.postTown,
      county = deliveryPointAddress.county,
      postcode = deliveryPointAddress.postcode,
      country = getCountry(deliveryPointAddress),
    )
  }

  private fun getCountry(deliveryPointAddress: DeliveryPointAddress) = deliveryPointAddress.countryDescription.split("\\s+".toRegex()).last()

  private fun getAddressFirstLine(deliveryPointAddress: DeliveryPointAddress): Pair<String, Boolean> {
    var addedLocalityToFirstLine = false
    with(deliveryPointAddress) {
      val firstLineBuilder = StringBuilder()
      val appendWithDelimiter = { value: Any -> firstLineBuilder.append(value).append(ADDRESS_ITEM_SEPARATOR) }
      organisationName?.let { appendWithDelimiter(it) }
      subBuildingName?.let { appendWithDelimiter(it) }
      buildingName?.let { appendWithDelimiter(it) }

      if (thoroughfareName.isNullOrEmpty()) {
        buildingNumber?.let { firstLineBuilder.append(it) }
        locality?.let {
          buildingNumber?.let { firstLineBuilder.append(ADDRESS_ITEM_SEPARATOR) }
          firstLineBuilder.append(it)
          addedLocalityToFirstLine = true
        }
      } else {
        buildingNumber?.let { firstLineBuilder.append(it).append(ADDRESS_ITEM_SEPARATOR) }
        firstLineBuilder.append(thoroughfareName)
      }

      return Pair(firstLineBuilder.toString(), addedLocalityToFirstLine)
    }
  }

  private fun getSecondLine(addedLocalityToFirstLine: Boolean, locality: String): String? {
    if (addedLocalityToFirstLine) {
      return null
    }
    return locality
  }
}
