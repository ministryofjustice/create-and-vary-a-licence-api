package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.AddressSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.OsPlacesMapperToAddressSearchResponseMapper
import java.time.LocalDate

class AddressSearchPaginatorTest {

  private val apiClient: OsPlacesApiClient = mock()
  private val mapper = OsPlacesMapperToAddressSearchResponseMapper()
  private val paginator = AddressSearchPaginator(apiClient, mapper, 50, 200)

  @Test
  fun `searchByText returns mapped results over multiple pages`() {
    // Given
    val expectedResultSize = 55
    stubTextSearch(expectedResultSize, "Street")

    // When
    val result = paginator.searchByText("Street")

    // Then
    assertResults(result, expectedResultSize)
  }

  @Test
  fun `searchByText returns empty when first page is empty`() {
    // Given
    whenever(apiClient.searchForAddressesByText(any(), eq("Street")))
      .thenReturn(emptyList())

    // When
    val result = paginator.searchByText("Street")

    // Then
    assertThat(result).isEmpty()
  }

  @Test
  fun `searchByText stops when page is partially filled`() {
    // Given
    val partialPage = listOf(
      createOsPlacesAddress(1),
      createOsPlacesAddress(2),
      createOsPlacesAddress(3),
    )
    whenever(apiClient.searchForAddressesByText(any(), eq("Street")))
      .thenReturn(partialPage)

    // When
    val result = paginator.searchByText("Street")

    // Then
    assertThat(result).hasSize(3)
    assertThat(result[0].uprn).isEqualTo("uprn-1")
    assertThat(result[2].uprn).isEqualTo("uprn-3")
  }

  @Test
  fun `searchByText limits to maxTotal`() {
    // Given
    stubTextSearch(250, query = "Street")

    // When
    val result = paginator.searchByText("Street")

    // Then
    assertThat(result).hasSize(200)
    assertThat(result.first().uprn).isEqualTo("uprn-1")
    assertThat(result.last().uprn).isEqualTo("uprn-200")
  }

  private fun assertResults(result: List<AddressSearchResponse>, expectedResultSize: Int) {
    assertThat(result).hasSize(expectedResultSize)
    result.forEachIndexed { i, addr ->
      val index = i + 1
      assertThat(addr.uprn).isEqualTo("uprn-$index")
      assertThat(addr.firstLine).contains("Building $index").contains("Street $index")
      assertThat(addr.secondLine).isEqualTo("Locality $index")
      assertThat(addr.townOrCity).isEqualTo("Town $index")
      assertThat(addr.county).isEqualTo("County $index")
      assertThat(addr.postcode).isEqualTo("PC${index}AA")
      assertThat(addr.country).isEqualTo("England")
    }
  }

  private fun stubTextSearch(totalResults: Int, query: String = "Street") {
    val pages = createResults(totalResults)
    whenever(apiClient.searchForAddressesByText(any(), eq(query)))
      .thenReturn(pages[0], *pages.drop(1).toTypedArray())
  }

  private fun createResults(totalResults: Int): List<List<DeliveryPointAddress>> {
    val pageSize = 50
    return (1..totalResults)
      .map(::createOsPlacesAddress)
      .chunked(pageSize)
  }

  private fun createOsPlacesAddress(index: Int): DeliveryPointAddress = DeliveryPointAddress(
    uprn = "uprn-$index",
    address = "Some Address $index",
    subBuildingName = null,
    organisationName = null,
    buildingName = "Building $index",
    buildingNumber = "$index",
    thoroughfareName = "Street $index",
    locality = "Locality $index",
    postTown = "Town $index",
    county = "County $index",
    postcode = "PC${index}AA",
    countryDescription = "England",
    xCoordinate = 123456.0 + index,
    yCoordinate = 654321.0 + index,
    lastUpdateDate = LocalDate.now(), // ← Set to today's date
  )
}
