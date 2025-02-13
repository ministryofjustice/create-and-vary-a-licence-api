package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class PrisonApiMockServer : WireMockServer(8091) {
  fun stubGetHdcLatest(bookingId: Long = 12345, approvalStatus: String = "REJECTED", passed: Boolean = true) {
    stubFor(
      get(urlEqualTo("/api/offender-sentences/booking/$bookingId/home-detention-curfews/latest")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                  "content": {
                      "approvalStatus": "$approvalStatus",
                      "passed": $passed,
                      "bookingId": $bookingId
                   }
               }""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetCourtOutcomes() {
    stubFor(
      post(urlEqualTo("/api/bookings/court-event-outcomes")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """[]""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetPrison() {
    stubFor(
      get(urlEqualTo("/api/agencies/prison/ABC")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
            "agencyId": "MDI",
            "formattedDescription": "Moorland",
            "phones": []
            }
          """.trimMargin(),
        ).withStatus(200),
      ),
    )
  }

  fun getHdcStatuses() {
    stubFor(
      post(urlEqualTo("/api/offender-sentences/home-detention-curfews/latest"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """[
                {
                  "bookingId": "123",
                  "passed": false
               },
               {
                  "bookingId": "456",
                  "passed": true
               },
               {
                  "bookingId": "789",
                  "passed": true
               },
               {
                  "bookingId": "123",
                  "approvalStatus": "APPROVED",
                  "passed": false
               }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }
}
