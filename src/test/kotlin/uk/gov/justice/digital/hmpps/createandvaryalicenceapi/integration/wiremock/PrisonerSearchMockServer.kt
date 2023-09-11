package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class PrisonerSearchMockServer : WireMockServer(8099) {
  fun stubSearchPrisonersByBookingIds() {
    stubFor(
      post(urlEqualTo("/api/prisoner-search/booking-ids"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """[
                {
                  "prisonerNumber": "G7285UT",
                  "bookingId": "456",
                  "status": "INACTIVE"
               },
                               {
                  "prisonerNumber": "G5613GT",
                  "bookingId": "789",
                  "status": "INACTIVE"
               },
                               {
                  "prisonerNumber": "G4169UO",
                  "bookingId": "432",
                  "status": "INACTIVE"
               }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }
}
