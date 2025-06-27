package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.DeliveryPointAddress
import java.time.LocalDate

/**
 * This unit test is to cover logic not covered in
 * @see uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.AddressSearchResourceIntegrationTest
 */
class OsPlacesMapperToAddressSearchResponseMapperTest {

  private val toTest: OsPlacesMapperToAddressSearchResponseMapper = OsPlacesMapperToAddressSearchResponseMapper()

  private companion object {
    @JvmStatic
    fun testMappingData(): List<Pair<DeliveryPointAddress, AddressSearchResponse>> {
      val testData = buildList {
        add(
          createMapFromAddress() to createExpectedMapTo(),
        )
        add(
          createMapFromAddress(organisationName = null)
            to
            createExpectedMapTo(firstLine = "ORDNANCE SUB, ORDNANCE HOUSE, 10, FAKE DRIVE"),
        )
        add(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
          ) to
            createExpectedMapTo(
              firstLine = "ORDNANCE HOUSE, 10, FAKE DRIVE",
            ),
        )
        add(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
          ) to
            createExpectedMapTo(
              firstLine = "10, FAKE DRIVE",
            ),
        )
        add(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
          ) to
            createExpectedMapTo(
              firstLine = "FAKE DRIVE",
            ),
        )
        add(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
            locality = null,
          ) to
            createExpectedMapTo(
              firstLine = "FAKE DRIVE",
              secondLine = null,
            ),
        )
        add(

          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
            locality = null,
            thoroughfareName = null,
          ) to
            createExpectedMapTo(
              firstLine = "",
              secondLine = null,
            ),
        )
        add(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            thoroughfareName = null,
          ) to
            createExpectedMapTo(
              firstLine = "10, FAKEALITY",
              secondLine = null,
            ),
        )
        add(
          createMapFromAddress(
            organisationName = null,
            subBuildingName = null,
            buildingName = null,
            buildingNumber = null,
            thoroughfareName = null,
          ) to
            createExpectedMapTo(
              firstLine = "FAKEALITY",
              secondLine = null,
            ),
        )
      }
      return testData
    }

    private fun createMapFromAddress(
      uprn: String = "100120991537",
      address: String = "10, THE STREET, FAKEALITY, FAKETON, FA1 1KE",
      subBuildingName: String? = "ORDNANCE SUB",
      organisationName: String? = "ORDNANCE SURVEY",
      buildingName: String? = "ORDNANCE HOUSE",
      buildingNumber: String? = "10",
      thoroughfareName: String? = "FAKE DRIVE",
      locality: String? = "FAKEALITY",
      postTown: String = "FAKETON",
      county: String = "FAKESHIRE",
      postcode: String = "FA11KE",
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
      firstLine: String = "ORDNANCE SURVEY, ORDNANCE SUB, ORDNANCE HOUSE, 10, FAKE DRIVE",
      secondLine: String? = "FAKEALITY",
      townOrCity: String = "FAKETON",
      county: String = "FAKESHIRE",
      postcode: String = "FA11KE",
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
