package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension

class RemandAndSentencingApiMockServer :
  WireMockExtension(
    extensionOptions()
      .options(wireMockConfig().port(8102)),
  ) {

  fun stubRemandStatusForRemandPrisoner(prisonNumber: String, isOnRemand: Boolean) {
    stubFor(
      get(urlEqualTo("/person/$prisonNumber/status")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                "isOnRemand": "$isOnRemand"
               }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubRemandStatusForAnyNonRemandPrisoners(isOnRemand: Boolean = false) {
    stubFor(
      get(urlPathMatching("/person/.*/status")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                "isOnRemand": "$isOnRemand"
               }""",
        ).withStatus(200),
      ),
    )
  }
}
