package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.address

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.OsPlacesMockServer
import java.nio.charset.StandardCharsets

private const val SEARCH_STRING = "Glan-y-mor"
private const val REFERENCE = "2345678"
private const val SEARCH_FOR_ADDRESSES_POST_URL = "/address/search/by/text/"
private const val GET_ADDRESS_BY_REFERENCE_URL = "/address/search/by/reference/$REFERENCE"
private const val OS_API_KEY = "os-places-api-key"

class AddressSearchControllerIntegrationTest : IntegrationTestBase() {

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

  abstract inner class BaseAddressSearchTestPost(private val urlToTest: String) {

    @Test
    fun `should return unauthorized if no token`() {
      webTestClient.post()
        .uri(urlToTest)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"searchQuery": "test"}""")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post()
        .uri(urlToTest)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation())
        .bodyValue("""{"searchQuery": "test"}""")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return forbidden if wrong role`() {
      webTestClient.post()
        .uri(urlToTest)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .bodyValue("""{"searchQuery": "test"}""")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `should return bad request with empty search query`() {
      webTestClient.post()
        .uri(urlToTest)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .bodyValue("""{"searchQuery": ""}""")
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.developerMessage").value<String> {
          assert(it.contains("Search query length must be more than 0 and no more than 100")) {
            "Expected developerMessage to contain validation error, but was: $it"
          }
        }
    }

    @Test
    fun `should return bad request with search query that is too long`() {
      webTestClient.post()
        .uri(urlToTest)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .bodyValue("""{"searchQuery": "Search query that is way too long and exceeds the maximum allowed length of one hundred characters which is not acceptable"}""")
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.developerMessage").value<String> {
          assert(it.contains("Search query length must be more than 0 and no more than 100")) {
            "Expected developerMessage to contain validation error, but was: $it"
          }
        }
    }
  }

  @Nested
  inner class SearchForAddressSearchPost : BaseAddressSearchTestPost(SEARCH_FOR_ADDRESSES_POST_URL) {

    @ParameterizedTest(name = "should return addresses with given search text for role {0}")
    @ValueSource(strings = ["ROLE_CVL_ADMIN"])
    fun `should return addresses results with given search text`(role: String) {
      // Given
      osPlacesMockServer.stubSearchForAddresses(SEARCH_STRING)

      // When
      val result = webTestClient.post()
        .uri(SEARCH_FOR_ADDRESSES_POST_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(role)))
        .bodyValue("""{"searchQuery": "$SEARCH_STRING"}""")
        .exchange()

      // Then

      result.expectStatus()
        .isOk
        .expectBody().json(serializedContent("address-by-search-text"), JsonCompareMode.STRICT)
    }
  }

  @Nested
  inner class GetAddressForReference : BaseAddressSearchTest(GET_ADDRESS_BY_REFERENCE_URL) {

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
