package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.DeliveryPointAddress

private const val ADDRESS_ITEM_SEPARATOR = ", "
private val KNOWN_COUNTRIES = listOf("England", "Scotland", "Wales", "Northern Ireland", "Ireland")

@Component
class OsPlacesMapperToAddressSearchResponseMapper {

  fun map(deliveryPointAddress: DeliveryPointAddress): AddressSearchResponse {
    var useThoroughfareInSecondLine = deliveryPointAddress.locality == null

    var firstLine = buildFirstLine(deliveryPointAddress, excludeThoroughfare = useThoroughfareInSecondLine)

    if (useThoroughfareInSecondLine && firstLine.isBlank()) {
      firstLine = buildFirstLine(deliveryPointAddress, excludeThoroughfare = false)
      useThoroughfareInSecondLine = false
    }

    val secondLine = buildSecondLine(deliveryPointAddress, firstLine, useThoroughfareInSecondLine)

    return AddressSearchResponse(
      uprn = deliveryPointAddress.uprn,
      firstLine = firstLine,
      secondLine = secondLine,
      townOrCity = deliveryPointAddress.postTown,
      county = deliveryPointAddress.county ?: "",
      postcode = deliveryPointAddress.postcode,
      country = getCountry(deliveryPointAddress),
    )
  }

  private fun getThoroughfareWithNumber(dpa: DeliveryPointAddress): String? = if (!dpa.thoroughfareName.isNullOrBlank()) {
    listOfNotNull(dpa.buildingNumber, dpa.thoroughfareName).joinToString(" ")
  } else {
    null
  }

  private fun buildFirstLine(dpa: DeliveryPointAddress, excludeThoroughfare: Boolean): String = listOfNotNull(
    dpa.subBuildingName,
    dpa.organisationName,
    dpa.buildingName,
    getBuildingNumberOrThoroughfare(dpa, excludeThoroughfare),
  ).joinToString(ADDRESS_ITEM_SEPARATOR)

  private fun buildSecondLine(dpa: DeliveryPointAddress, firstLine: String, useThoroughfare: Boolean): String? = when {
    useThoroughfare && firstLine.isNotBlank() -> getThoroughfareWithNumber(dpa)
    dpa.locality != null -> dpa.locality
    else -> null
  }

  private fun getBuildingNumberOrThoroughfare(dpa: DeliveryPointAddress, excludeThoroughfare: Boolean): String? = when {
    excludeThoroughfare && dpa.thoroughfareName == null -> dpa.buildingNumber
    excludeThoroughfare -> null
    dpa.thoroughfareName == null -> dpa.buildingNumber
    else -> getThoroughfareWithNumber(dpa)
  }

  private fun getCountry(dpa: DeliveryPointAddress): String {
    val desc = dpa.countryDescription.trim()
    return KNOWN_COUNTRIES.firstOrNull { desc.contains(it, ignoreCase = true) } ?: ""
  }
}
