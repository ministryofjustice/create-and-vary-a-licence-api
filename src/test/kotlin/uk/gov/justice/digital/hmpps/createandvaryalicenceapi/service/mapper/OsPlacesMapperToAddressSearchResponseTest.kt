package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.DeliveryPointAddress
import java.time.LocalDate

/**
 * This unit test is to cover logic not covered in
 *  {@link uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.AddressResourceIntegrationTest } class
 */
class OsPlacesMapperToAddressSearchResponseTest {

  private val toTest: OsPlacesMapperToAddressSearchResponse = OsPlacesMapperToAddressSearchResponse()

  private companion object {
    @JvmStatic
    fun testMappingData(): List<Pair<DeliveryPointAddress, AddressSearchResponse>> {
      val testData = mutableListOf<Pair<DeliveryPointAddress, AddressSearchResponse>>()

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(),
          createExpectedMapTo(),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(organisationName = null),
          createExpectedMapTo(firstLine = "ORDNANCE SUB, ORDNANCE HOUSE, 10, ADANAC DRIVE"),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
          ),
          createExpectedMapTo(
            firstLine = "ORDNANCE HOUSE, 10, ADANAC DRIVE",
          ),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
          ),
          createExpectedMapTo(
            firstLine = "10, ADANAC DRIVE",
          ),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
          ),
          createExpectedMapTo(
            firstLine = "ADANAC DRIVE",
          ),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
            locality = null,
          ),
          createExpectedMapTo(
            firstLine = "ADANAC DRIVE",
            secondLine = null,
          ),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
            locality = null,
            thoroughfareName = null,
          ),
          createExpectedMapTo(
            firstLine = "",
            secondLine = null,
          ),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            thoroughfareName = null,
          ),
          createExpectedMapTo(
            firstLine = "10, MORTIMER",
            secondLine = null,
          ),
        ),
      )

      testData.add(
        Pair<DeliveryPointAddress, AddressSearchResponse>(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
            thoroughfareName = null,
          ),
          createExpectedMapTo(
            firstLine = "MORTIMER",
            secondLine = null,
          ),
        ),
      )

      return testData
    }

    private fun createMapFromAddress(
      uprn: String = "100120991537",
      address: String = "10, THE STREET, MORTIMER, READING, AG12 1RW",
      subBuildingName: String? = "ORDNANCE SUB",
      organisationName: String? = "ORDNANCE SURVEY",
      buildingName: String? = "ORDNANCE HOUSE",
      buildingNumber: String? = "10",
      thoroughfareName: String? = "ADANAC DRIVE",
      locality: String? = "MORTIMER",
      postTown: String = "READING",
      county: String = "WILTSHIRE",
      postcode: String = "SA420UQ",
      countryDescription: String = "This record is within England",
      xCoordinate: Double = Double.MIN_VALUE,
      yCoordinate: Double = Double.MAX_VALUE,
      lastUpdateDate: LocalDate = LocalDate.now(),
    ) = DeliveryPointAddress(
      uprn = uprn,
      address = address,
      subBuildingName = subBuildingName,
      organisationName = organisationName,
      buildingName = buildingName,
      buildingNumber = buildingNumber,
      thoroughfareName = thoroughfareName,
      locality = locality,
      postTown = postTown,
      county = county,
      postcode = postcode,
      countryDescription = countryDescription,
      xCoordinate = xCoordinate,
      yCoordinate = yCoordinate,
      lastUpdateDate = lastUpdateDate,
    )

    private fun createExpectedMapTo(
      reference: String = "100120991537",
      firstLine: String = "ORDNANCE SURVEY, ORDNANCE SUB, ORDNANCE HOUSE, 10, ADANAC DRIVE",
      secondLine: String? = "MORTIMER",
      townOrCity: String = "READING",
      county: String = "WILTSHIRE",
      postcode: String = "SA420UQ",
      country: String = "England",
    ) = AddressSearchResponse(
      reference = reference,
      firstLine = firstLine,
      secondLine = secondLine,
      townOrCity = townOrCity,
      county = county,
      postcode = postcode,
      country = country,
    )
  }

  @ParameterizedTest
  @MethodSource("testMappingData")
  fun `should map search address`(testData: Pair<DeliveryPointAddress, AddressSearchResponse>) {
    // When
    val result = toTest.map(testData.first)

    // Then
    assertThat(result).isEqualTo(testData.second)
  }
}
