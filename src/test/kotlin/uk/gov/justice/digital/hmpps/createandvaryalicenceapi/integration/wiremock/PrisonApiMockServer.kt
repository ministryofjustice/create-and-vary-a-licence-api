package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import java.time.LocalDate

class PrisonApiMockServer : WireMockServer(8091) {

  companion object {
    val objectMapper = ObjectMapper().apply {
      registerModule(Jdk8Module())
      registerModule(JavaTimeModule())
      registerKotlinModule()
    }
  }

  fun stubGetHdcLatest(bookingId: Long = 12345, approvalStatus: String = "REJECTED", passed: Boolean = true) {
    stubFor(
      get(urlEqualTo("/api/offender-sentences/booking/$bookingId/home-detention-curfews/latest")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
                "approvalStatus": "$approvalStatus",
                "passed": $passed,
                "bookingId": $bookingId
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

  fun stubGetPrisonerDetail(nomsId: String = "A1234AA", releaseDate: LocalDate? = LocalDate.of(2021, 10, 22)) {
    stubFor(
      get(urlEqualTo("/api/offenders/$nomsId")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """{
            "offenderNo": "A1234AA",
            "firstName": "A",
            "lastName": "Prisoner",
            "dateOfBirth": "1985-12-28",
            "bookingId": 123456,
            "offenceHistory": [
              {
                "offenceDescription": "SOME_OFFENCE",
                "offenceCode": "123",
                "mostSerious": true
               }
            ],
            "legalStatus": "SENTENCED",
            "sentenceDetail": {
              "confirmedReleaseDate": "$releaseDate",
              "conditionalReleaseDate": "$releaseDate",
              "homeDetentionCurfewEligibilityDate": null,
              "homeDetentionCurfewActualDate": "2024-08-01",
              "topupSupervisionStartDate": null,
              "topupSupervisionExpiryDate": null,
              "paroleEligibilityDate": null
            }
          }
          """.trimMargin(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetPrisonerDetail(nomsId: String = "A1234AA", sentenceDetail: SentenceDetail) {
    val json = """{
            "offenderNo": "A1234AA",
            "firstName": "A",
            "lastName": "Prisoner",
            "dateOfBirth": "1985-12-28",
            "bookingId": 123456,
            "offenceHistory": [
              {
                "offenceDescription": "SOME_OFFENCE",
                "offenceCode": "123",
                "mostSerious": true
               }
            ],
            "legalStatus": "SENTENCED",
            "sentenceDetail": ${objectMapper.writeValueAsString(sentenceDetail)}
          }
          """
    stubFor(
      get(urlEqualTo("/api/offenders/$nomsId")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          json.trimMargin(),
        ).withStatus(200),
      ),
    )
  }
}
