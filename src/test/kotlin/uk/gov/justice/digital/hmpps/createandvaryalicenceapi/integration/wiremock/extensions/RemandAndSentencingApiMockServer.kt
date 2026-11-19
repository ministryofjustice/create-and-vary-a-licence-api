package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing.PersonStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing.PersonStatusesResponse

class RemandAndSentencingApiMockServer :
  WireMockExtension(
    extensionOptions()
      .options(wireMockConfig().port(8102)),
  ) {

  private val objectMapper = ObjectMapper().registerKotlinModule()

  fun stubRemandStatuses(remandPrisonerNumbers: Set<String>) {
    val peopleStatus =
      remandPrisonerNumbers.associateWith { PersonStatus(isOnRemand = true, isCurrentlyOnRecall = false) }

    stubFor(
      post(urlEqualTo("/person/statuses")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          objectMapper.writeValueAsString(PersonStatusesResponse(peopleStatus)),
        ).withStatus(200),
      ),
    )
  }
}
