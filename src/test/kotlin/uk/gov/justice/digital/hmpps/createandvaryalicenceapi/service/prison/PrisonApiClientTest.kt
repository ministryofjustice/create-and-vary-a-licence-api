package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus

class PrisonApiClientTest {

  companion object {
    @RegisterExtension
    val wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()
    val objectMapper = ObjectMapper().apply {
      registerModule(Jdk8Module())
      registerModule(JavaTimeModule())
      registerKotlinModule()
    }
  }

  lateinit var prisonApiClient: PrisonApiClient

  @BeforeEach
  fun reset() {
    val webClient = WebClient.builder().baseUrl("http://localhost:${wiremock.port}").build()
    prisonApiClient = PrisonApiClient(webClient)
  }

  @Test
  fun `search by teams endpoint not called if no teams`() {
    val page1 = MutableList(4) { i -> hdcPrisonerStatus().copy(bookingId = i.toLong() + 1 * 10) }
    val page2 = MutableList(4) { i -> hdcPrisonerStatus().copy(bookingId = i.toLong() + 1 * 20) }
    val page3 = MutableList(2) { i -> hdcPrisonerStatus().copy(bookingId = i.toLong() + 1 * 30) }

    stubPage(thisScenario = STARTED, nextScenario = "Page1", response = page1)
    stubPage(thisScenario = "Page1", nextScenario = "Page2", response = page2)
    stubPage(thisScenario = "Page2", nextScenario = "Page3", response = page3)

    val bookingIds = MutableList(10) { it.toLong() }
    val response = prisonApiClient.getHdcStatuses(bookingIds, 4)

    assertThat(response).hasSize(10)
    assertThat(response).extracting<Int> { it.bookingId!!.toInt() }
      .isEqualTo(listOf(10, 11, 12, 13, 20, 21, 22, 23, 30, 31))
  }

  private fun stubPage(thisScenario: String, nextScenario: String, response: List<PrisonerHdcStatus>) {
    wiremock.stubFor(
      post("/offender-sentences/home-detention-curfews/latest")
        .inScenario("Retry Scenario")
        .whenScenarioStateIs(thisScenario)
        .willReturn(
          aResponse().withHeader("Content-Type", "application/json").withBody(
            objectMapper.writeValueAsString(response),
          ),
        ).willSetStateTo(nextScenario),
    )
  }
}
