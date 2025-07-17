import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.DeliveryPointAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponseMapper
import java.time.LocalDate

class OsPlacesMapperToAddressSearchResponseMapperTest {

  private val mapper = OsPlacesMapperToAddressSearchResponseMapper()

  @Nested
  inner class MapTests {

    @Test
    fun `given empty first line and locality is null when map then thoroughfare is on first line and second is null`() {
      // Given
      val dpa = createAddress(
        uprn = "1000",
        buildingNumber = "221B",
        thoroughfareName = "Baker Street",
        locality = null,
        subBuildingName = null,
        organisationName = null,
        buildingName = null
      )

      // When
      val result = mapper.map(dpa)

      // Then
      assertThat(result.firstLine).isEqualTo("221B Baker Street")
      assertThat(result.secondLine).isNull()
      assertThat(result.country).isEqualTo("England")
    }

    @Test
    fun `given non-empty first line and no locality when map then thoroughfare is on second line`() {
      // Given
      val dpa = createAddress(
        uprn = "1001",
        subBuildingName = "Flat 3B",
        buildingNumber = "10",
        thoroughfareName = "Downing Street",
        locality = null,
        countryDescription = "Scotland"
      )

      // When
      val result = mapper.map(dpa)

      // Then
      assertThat(result.firstLine).isEqualTo("Flat 3B")
      assertThat(result.secondLine).isEqualTo("10 Downing Street")
      assertThat(result.country).isEqualTo("Scotland")
    }

    @Test
    fun `given locality is present when map then thoroughfare in first line and locality in second line`() {
      // Given
      val dpa = createAddress(
        uprn = "1002",
        organisationName = "Globex Corp",
        buildingName = "Innovation Tower",
        buildingNumber = "5",
        thoroughfareName = "Elm Street",
        locality = "North Industrial Zone",
        countryDescription = "Wales"
      )

      // When
      val result = mapper.map(dpa)

      // Then
      assertThat(result.firstLine).isEqualTo("Globex Corp, Innovation Tower, 5 Elm Street")
      assertThat(result.secondLine).isEqualTo("North Industrial Zone")
      assertThat(result.country).isEqualTo("Wales")
    }

    @Test
    fun `given thoroughfareName and locality are null when map then only building number in first line`() {
      // Given
      val dpa = createAddress(
        uprn = "1003",
        buildingNumber = "42",
        thoroughfareName = null,
        locality = null,
        countryDescription = "Northern Ireland"
      )

      // When
      val result = mapper.map(dpa)

      // Then
      assertThat(result.firstLine).isEqualTo("42")
      assertThat(result.secondLine).isNull()
      assertThat(result.country).isEqualTo("Northern Ireland")
    }

    @Test
    fun `given country is not recognised when map then country is empty`() {
      // Given
      val dpa = createAddress(
        uprn = "1004",
        thoroughfareName = "Kingfisher Way",
        locality = "Greenfields",
        countryDescription = "Atlantis"
      )

      // When
      val result = mapper.map(dpa)

      // Then
      assertThat(result.country).isEmpty()
    }

    @Test
    fun `given all optional fields null when map then result still has default town county postcode`() {
      // Given
      val dpa = createAddress(
        uprn = "1005",
        subBuildingName = null,
        organisationName = null,
        buildingName = null,
        buildingNumber = null,
        thoroughfareName = null,
        locality = null,
        countryDescription = ""
      )

      // When
      val result = mapper.map(dpa)

      // Then
      assertThat(result.firstLine).isEmpty()
      assertThat(result.secondLine).isNull()
      assertThat(result.townOrCity).isEqualTo("Faketown")
      assertThat(result.county).isEqualTo("Westshire")
      assertThat(result.postcode).isEqualTo("FK1 2ZZ")
      assertThat(result.country).isEmpty()
    }
  }

  private fun createAddress(
    uprn: String,
    address: String = "1 Some Street",
    subBuildingName: String? = null,
    organisationName: String? = null,
    buildingName: String? = null,
    buildingNumber: String? = null,
    thoroughfareName: String? = "Mock Road",
    locality: String? = null,
    postTown: String = "Faketown",
    county: String = "Westshire",
    postcode: String = "FK1 2ZZ",
    countryDescription: String = "England",
    xCoordinate: Double = Double.MAX_VALUE,
    yCoordinate: Double = Double.MIN_VALUE,
    lastUpdateDate: LocalDate = LocalDate.now()
  ): DeliveryPointAddress {
    return DeliveryPointAddress(
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
      lastUpdateDate = lastUpdateDate
    )
  }
}
