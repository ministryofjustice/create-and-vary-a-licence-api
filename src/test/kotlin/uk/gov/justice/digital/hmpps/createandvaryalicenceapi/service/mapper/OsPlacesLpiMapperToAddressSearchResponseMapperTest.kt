import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.dto.LocalPropertyIdentifierAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesLpiMapperToAddressSearchResponseMapper
import java.time.LocalDate

class OsPlacesLpiMapperToAddressSearchResponseMapperTest {

  private val mapper = OsPlacesLpiMapperToAddressSearchResponseMapper()

  @Nested
  inner class MapTests {

    @Test
    fun `given address starts with a number when map then first two address items are joined on first line`() {
      // Given
      val address = createAddress(
        uprn = "766258743",
        address = "1, Some Street, Aylesbury",
        postTown = "AYLESBURY",
        county = "BUCKINGHAMSHIRE",
        postcode = "HP21 7RL",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.uprn).isEqualTo("766258743")
      assertThat(result.firstLine).isEqualTo("1 Some Street")
      assertThat(result.secondLine).isEmpty()
      assertThat(result.townOrCity).isEqualTo("AYLESBURY")
      assertThat(result.county).isEqualTo("BUCKINGHAMSHIRE")
      assertThat(result.postcode).isEqualTo("HP21 7RL")
      assertThat(result.country).isEqualTo("England")
    }

    @Test
    fun `given address does not start with a number when map then first address item is used as first line`() {
      // Given
      val address = createAddress(
        uprn = "766258879",
        address = "Unit 2B, Wynne Jones Centre, WALTON ROAD",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.firstLine).isEqualTo("Unit 2B")
      assertThat(result.secondLine).isEqualTo("Wynne Jones Centre, WALTON ROAD")
    }

    @Test
    fun `given address starts with a number and has multiple items when map then remaining items are second line`() {
      // Given
      val address = createAddress(
        uprn = "766302598",
        address = "10, Unit 2, Wynne Jones Centre, WALTON ROAD",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.firstLine).isEqualTo("10 Unit 2")
      assertThat(result.secondLine).isEqualTo("Wynne Jones Centre, WALTON ROAD")
    }

    @Test
    fun `given address is empty and first line components are null when map then first line is empty`() {
      // Given
      val address = createAddress(
        uprn = "1002",
        address = "",
        buildingNumber = null,
        subBuildingName = null,
        buildingName = null,
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.firstLine).isEmpty()
      assertThat(result.secondLine).isEmpty()
    }

    @Test
    fun `given postcode is at end of address when map then postcode is removed`() {
      // Given
      val address = createAddress(
        uprn = "1003",
        address = "1, Some Street, FK1 2ZZ",
        postcode = "FK1 2ZZ",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.firstLine).isEqualTo("1 Some Street")
      assertThat(result.secondLine).isEmpty()
    }

    @Test
    fun `given county is at end of address when map then county is removed`() {
      // Given
      val address = createAddress(
        uprn = "1004",
        address = "1, Some Street, Westshire",
        county = "Westshire",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.firstLine).isEqualTo("1 Some Street")
      assertThat(result.secondLine).isEmpty()
    }

    @Test
    fun `given post town is at end of address when map then post town is removed`() {
      // Given
      val address = createAddress(
        uprn = "1005",
        address = "1, Some Street, Faketown",
        postTown = "Faketown",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.firstLine).isEqualTo("1 Some Street")
      assertThat(result.secondLine).isEmpty()
    }

    @Test
    fun `given post town is null when map then town or city is empty`() {
      // Given
      val address = createAddress(
        uprn = "1006",
        postTown = null,
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.townOrCity).isEmpty()
    }

    @Test
    fun `given county is null when map then county is empty`() {
      // Given
      val address = createAddress(
        uprn = "1007",
        county = null,
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.county).isEmpty()
    }

    @Test
    fun `given country description contains recognised country when map then country is returned`() {
      // Given
      val address = createAddress(
        uprn = "1008",
        countryDescription = "This record is within England",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.country).isEqualTo("England")
    }

    @Test
    fun `given country description has surrounding whitespace when map then country is returned`() {
      // Given
      val address = createAddress(
        uprn = "1009",
        countryDescription = "  This record is within Scotland  ",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.country).isEqualTo("Scotland")
    }

    @Test
    fun `given country description has different case when map then country is returned`() {
      // Given
      val address = createAddress(
        uprn = "1010",
        countryDescription = "THIS RECORD IS WITHIN WALES",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.country).isEqualTo("Wales")
    }

    @Test
    fun `given country is not recognised when map then country is empty`() {
      // Given
      val address = createAddress(
        uprn = "1011",
        countryDescription = "This record is within Atlantis",
      )

      // When
      val result = mapper.map(address)

      // Then
      assertThat(result.country).isEmpty()
    }
  }

  private fun createAddress(
    uprn: String,
    address: String = "1, Some Street",
    subBuildingName: String? = null,
    organisationName: String? = null,
    buildingName: String? = null,
    buildingNumber: String? = null,
    thoroughfareName: String? = "Mock Road",
    locality: String? = null,
    postTown: String? = "Faketown",
    county: String? = "Westshire",
    postcode: String = "FK1 2ZZ",
    countryDescription: String = "This record is within England",
    xCoordinate: Double = Double.MAX_VALUE,
    yCoordinate: Double = Double.MIN_VALUE,
    lastUpdateDate: LocalDate = LocalDate.now(),
  ): LocalPropertyIdentifierAddress = LocalPropertyIdentifierAddress(
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
}
