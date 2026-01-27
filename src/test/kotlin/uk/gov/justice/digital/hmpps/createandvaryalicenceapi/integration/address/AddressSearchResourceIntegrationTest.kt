package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.address

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.OsPlacesMockServer
import java.nio.charset.StandardCharsets

private const val SEARCH_STRING = "Glan-y-mor"
private const val REFERENCE = "2345678"
private const val SEARCH_FOR_ADDRESSES_URL = "/address/search/by/text/$SEARCH_STRING"
private const val GET_ADDRESS_BY_REFERENCE_URL = "/address/search/by/reference/$REFERENCE"
private const val OS_API_KEY = "os-places-api-key"

class AddressSearchResourceIntegrationTest : IntegrationTestBase() {

  private companion object {
    val osPlacesMockServer = OsPlacesMockServer(OS_API_KEY)

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      osPlacesMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      osPlacesMockServer.stop()
    }
  }

  abstract inner class BaseAddressSearchTest(private val urlToTest: String) {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.get()
        .uri(urlToTest)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.get()
        .uri(urlToTest)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.get()
        .uri(urlToTest)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class SearchForAddressSearch : BaseAddressSearchTest(SEARCH_FOR_ADDRESSES_URL) {

    @ParameterizedTest(name = "should return addresses with given search text for role {0}")
    @ValueSource(strings = ["ROLE_CVL_ADMIN"])
    fun `should return addresses results with given search text`(role: String) {
      // Given
      osPlacesMockServer.stubSearchForAddresses(SEARCH_STRING)

      // When
      val result = webTestClient.get()
        .uri(SEARCH_FOR_ADDRESSES_URL)
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()

      // Then

      result.expectStatus()
        .isOk
        .expectBody().json(serializedContent("address-by-search-text"), JsonCompareMode.STRICT)
    }
  }

  @Nested
  inner class GetAddressForReference : BaseAddressSearchTest(SEARCH_FOR_ADDRESSES_URL) {

    @ParameterizedTest(name = "should get an address for given Reference for role {0}")
    @ValueSource(strings = ["ROLE_CVL_ADMIN"])
    fun `should get an address for given Reference`(role: String) {
      // Given
      osPlacesMockServer.stubGetAddressByUprn(REFERENCE)

      // When
      val result = webTestClient.get()
        .uri(GET_ADDRESS_BY_REFERENCE_URL)
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()

      //  Then
      result.expectStatus()
        .isOk
        .expectBody().json(serializedContent("address-by-reference"), JsonCompareMode.STRICT)
    }
  }

  private fun serializedContent(name: String) = this.javaClass.getResourceAsStream("/test_data/address/responses/$name.json")!!.bufferedReader(
    StandardCharsets.UTF_8,
  ).readText()
}
