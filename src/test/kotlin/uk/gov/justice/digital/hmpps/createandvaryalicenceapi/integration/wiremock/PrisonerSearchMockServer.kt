package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import java.time.LocalDate

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
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topUpSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now().plusDays(1)}",
                  "confirmedReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "conditionalReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false
               },
               {
                  "prisonerNumber": "A1234AB",
                  "bookingId": "456",
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topUpSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false
               },
               {
                  "prisonerNumber": "A1234AC",
                  "bookingId": "789",
                  "status": "INACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": null,
                  "topUpSupervisionExpiryDate": null,
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": null,
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false
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
