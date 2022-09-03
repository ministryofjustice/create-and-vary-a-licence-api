package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class PrisonApiMockServer : WireMockServer(8091) {
  fun stubGetHdcLatest(bookingId: Long = 12345, approvalStatus: String = "REJECTED", passed: Boolean = true) {
    stubFor(
      get(urlEqualTo("/offender-sentences/booking/$bookingId/home-detention-curfews/latest")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                  "content": {
                      "approvalStatus": "$approvalStatus",
                      "passed": $passed,
                      "bookingId": $bookingId
                   }
               }"""
        ).withStatus(200)
      )
    )
  }
}
