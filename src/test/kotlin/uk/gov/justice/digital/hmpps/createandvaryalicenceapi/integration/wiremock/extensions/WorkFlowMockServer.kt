package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension

class WorkFlowMockServer :
  WireMockExtension(
    extensionOptions()
      .options(wireMockConfig().port(8101)),
  ) {

  fun stubGetStaffDetails(personUuid: String, crn: String, staffCode: String, teamCode: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/allocation/person/$personUuid")).willReturn(
        WireMock.aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
              "id": "$personUuid",
              "staffCode": "$staffCode",
              "teamCode": "$teamCode",
              "createdDate": "2025-07-28T09:30:00",
              "crn": "$crn"
          }""",
        ).withStatus(200),
      ),
    )
  }
}
