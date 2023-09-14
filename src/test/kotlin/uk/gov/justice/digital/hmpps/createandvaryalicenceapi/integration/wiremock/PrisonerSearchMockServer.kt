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

  fun stubSearchPrisonersByNomisIds() {
    stubFor(
      post(urlEqualTo("/api/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """[
                {
                  "prisonerNumber": "A1234AA",
                  "bookingId": "123",
                  "status": "INACTIVE",
                  "licenceExpiryDate": "2024-09-14",
                  "topUpSupervisionExpiryDate": "2024-09-14",
                  "releaseDate": "2023-09-14",
                  "confirmedReleaseDate": "2023-09-14"
               },
                               {
                  "prisonerNumber": "A1234AB",
                  "bookingId": "456",
                  "status": "INACTIVE",
                  "licenceExpiryDate": "2024-09-14",
                  "topUpSupervisionExpiryDate": "2024-09-14",
                  "releaseDate": null,
                  "confirmedReleaseDate": "2023-09-14"
               },
               {
                  "prisonerNumber": "A1234AC",
                  "bookingId": "789",
                  "status": "INACTIVE",
                  "licenceExpiryDate": null,
                  "topUpSupervisionExpiryDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null
               }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }

  fun stubSearchPrisonersByNomisIdsNoResult() {
    stubFor(
      post(urlEqualTo("/api/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """[]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }
}
