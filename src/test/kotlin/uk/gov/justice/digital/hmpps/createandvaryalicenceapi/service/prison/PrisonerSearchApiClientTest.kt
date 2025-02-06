package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import java.time.LocalDate

class PrisonerSearchApiClientTest {

  companion object {
    @RegisterExtension
    val wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()
    val objectMapper = ObjectMapper().apply {
      registerModule(Jdk8Module())
      registerModule(JavaTimeModule())
      registerKotlinModule()
    }
  }

  lateinit var prisonerSearchApiClient: PrisonerSearchApiClient

  @BeforeEach
  fun reset() {
    var webClient = WebClient.builder().baseUrl("http://localhost:${wiremock.port}").build()
    prisonerSearchApiClient = PrisonerSearchApiClient(webClient)
  }

  @Test
  fun `search by teams endpoint not called if no teams`() {
    stubPage(pageNumber = 0, foundRecordsOnThisPage = 4, pageSize = 4, totalElements = 10)
    stubPage(pageNumber = 1, foundRecordsOnThisPage = 4, pageSize = 4, totalElements = 10)
    stubPage(pageNumber = 2, foundRecordsOnThisPage = 2, pageSize = 4, totalElements = 10)

    val response =
      prisonerSearchApiClient.getAllByReleaseDate(LocalDate.now(), LocalDate.now().plusWeeks(4), pageSize = 4)

    assertThat(response).hasSize(10)
    assertThat(response).extracting<String> { it.bookingId }
      .isEqualTo(listOf("0-0", "0-1", "0-2", "0-3", "1-0", "1-1", "1-2", "1-3", "2-0", "2-1"))
  }

  private fun stubPage(pageNumber: Int, foundRecordsOnThisPage: Int, pageSize: Int, totalElements: Int) {
    val foundResults =
      MutableList(foundRecordsOnThisPage) { i -> prisonerSearchResult().copy(bookingId = "$pageNumber-$i") }
    wiremock.stubFor(
      post("/prisoner-search/release-date-by-prison?size=$pageSize&page=$pageNumber")
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json").withBody(
            """
              { "content": ${objectMapper.writeValueAsString(foundResults)}, "number": $pageNumber, "size": $pageSize, "totalElements": $totalElements }
              """,
          ),
        ),
    )
  }
}
