package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
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
  fun `get the person statuses for a list of prison numbers`() {
    val prisonNumber = "A1234AA"
    val personStatus = PersonStatus(isOnRemand = true, isCurrentlyOnRecall = false)

    wiremock.stubFor(
      post(urlEqualTo("/person/statuses"))
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(PersonStatusesRequest(listOf(prisonNumber)))))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              objectMapper.writeValueAsString(
                PersonStatusesResponse(mapOf(prisonNumber to personStatus)),
              ),
            )
            .withStatus(200),
        ),
    )

    val response = remandAndSentencingApiClient.getPersonStatuses(listOf(prisonNumber))

    assertThat(response).isEqualTo(PersonStatusesResponse(mapOf(prisonNumber to personStatus)))
  }
}
