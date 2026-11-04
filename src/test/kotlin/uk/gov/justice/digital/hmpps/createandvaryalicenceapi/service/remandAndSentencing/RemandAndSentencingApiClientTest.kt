package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.web.reactive.function.client.WebClient

class RemandAndSentencingApiClientTest {

  companion object {
    @RegisterExtension
    val wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build()
    val objectMapper = ObjectMapper().apply {
      registerModule(Jdk8Module())
      registerModule(JavaTimeModule())
      registerKotlinModule()
    }
  }

  lateinit var remandAndSentencingApiClient: RemandAndSentencingApiClient

  @BeforeEach
  fun reset() {
    val webClient = WebClient.builder().baseUrl("http://localhost:${wiremock.port}").build()
    remandAndSentencingApiClient = RemandAndSentencingApiClient(webClient)
  }

  @Test
  fun `get the remand outcome for a prison number`() {
    val prisonNumber = "A1234AA"
    val remandOutcome = RemandOutcome(isOnRemand = true)

    wiremock.stubFor(
      get(urlEqualTo("/person/$prisonNumber/status")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(objectMapper.writeValueAsString(remandOutcome))
          .withStatus(200),
      ),
    )

    val response = remandAndSentencingApiClient.getRemandOutcome(prisonNumber)

    assertThat(response).isEqualTo(remandOutcome)
  }
}
