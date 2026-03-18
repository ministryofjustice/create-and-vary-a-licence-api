package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

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
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.createMapper

class PrisonApiClientTest {

  companion object {
    @RegisterExtension
    val wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()
    private val mapper: ObjectMapper = createMapper()
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
            mapper.writeValueAsString(response),
          ),
        ).willSetStateTo(nextScenario),
    )
  }
}
