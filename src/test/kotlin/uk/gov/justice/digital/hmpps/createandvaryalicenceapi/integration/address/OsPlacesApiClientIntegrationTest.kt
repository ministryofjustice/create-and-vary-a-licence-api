package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.address

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.addressSearch.OsPlacesApiClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class OsPlacesApiClientIntegrationTest {

  @Test
  fun `should url encode search query when calling os places`() {
    // Given
    val searchQuery = "Glan-y-mor"
    val encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)

    createWireMock(encodedQuery)

    val webClient = WebClient.builder()
      .baseUrl(wireMock.baseUrl())
      .build()

    val client = OsPlacesApiClient(
      osPlacesApiWebClient = webClient,
      apiKey = API_KEY,
    )

    val pageable = PageRequest.of(0, 10)

    // When
    client.searchForAddressesByText(pageable, searchQuery)

    // Then
    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/find"))
        .withQueryParam("query", equalTo(encodedQuery)),
    )
  }

  @Test
  fun `should handle missing LOCAL_CUSTODIAN_CODE_DESCRIPTION in os places response`() {
    // Given
    val searchQuery = "Glan-y-mor"
    val encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)

    createWireMock(encodedQuery, localCustodianCodeDescription = null)

    val webClient = WebClient.builder()
      .baseUrl(wireMock.baseUrl())
      .build()

    val client = OsPlacesApiClient(
      osPlacesApiWebClient = webClient,
      apiKey = API_KEY,
    )

    val pageable = PageRequest.of(0, 10)

    // When
    client.searchForAddressesByText(pageable, searchQuery)

    // Then
    wireMock.verify(
      getRequestedFor(urlPathEqualTo("/find"))
        .withQueryParam("query", equalTo(encodedQuery)),
    )
  }

  private fun createWireMock(
    encodedQuery: String?,
    localCustodianCodeDescription: String? = "Pembrokeshire",
  ) {
    // Given
    val dpa = mutableMapOf<String, Any>(
      "UPRN" to "1234567890",
      "ADDRESS" to "1 Glan-y-mor",
      "POST_TOWN" to "Tenby",
      "POSTCODE" to "SA70 7AA",
      "COUNTRY_CODE_DESCRIPTION" to "Wales",
      "X_COORDINATE" to 123456.78,
      "Y_COORDINATE" to 987654.32,
      "LAST_UPDATE_DATE" to "01/01/2024",
    )

    localCustodianCodeDescription?.let {
      dpa["LOCAL_CUSTODIAN_CODE_DESCRIPTION"] = it
    }

    val response = mapOf(
      "results" to listOf(
        mapOf("DPA" to dpa),
      ),
    )

    val json = jacksonObjectMapper().writeValueAsString(response)

    wireMock.stubFor(
      get(urlPathEqualTo("/find"))
        .withQueryParam("query", equalTo(encodedQuery))
        .withQueryParam("key", equalTo(API_KEY))
        .willReturn(
          okJson(json)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE),
        ),
    )
  }

  companion object {
    private const val API_KEY = "test-api-key"

    @JvmField
    @RegisterExtension
    val wireMock: WireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build()
  }
}
