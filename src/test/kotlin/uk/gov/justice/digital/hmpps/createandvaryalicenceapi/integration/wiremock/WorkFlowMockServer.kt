package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class WorkFlowMockServer : WireMockServer(8101) {

  fun stubGetStaffDetails(personUuid: String, crn: String, staffCode: String, teamCode: String) {
    stubFor(
      get(urlEqualTo("/allocation/person/$personUuid")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
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
