package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderSentencesAndOffences
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.BookingSentenceAndRecallTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.OffenderSentenceAndOffences
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper
import java.time.LocalDate

class PrisonApiMockServer : WireMockServer(8091) {

  companion object {
    private val mapper: ObjectMapper = createTestMapper()
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
      post(urlEqualTo("/api/bookings/court-event-outcomes?outcomeReasonCodes=3006,4022,5500,5502")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """[]""",
        ).withStatus(200),
      ),
    )
  }

  fun stubGetPrison(prisonId: String = "ABC") {
    stubFor(
      get(urlEqualTo("/api/agencies/prison/$prisonId")).willReturn(
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
            "agencyId": "ABC",
            "legalStatus": "SENTENCED",
            "status": "ACTIVE IN",
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
            "sentenceDetail": ${mapper.writeValueAsString(sentenceDetail)},
            "agencyId": "ABC",
            "status": "ACTIVE IN"
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

  fun stubGetSentenceAndRecallTypes(bookingId: Long = 123) {
    stubFor(
      post(urlEqualTo("/api/offender-sentences/bookings/sentence-and-recall-types")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """[{
            "bookingId": "$bookingId",
            "sentenceTypeRecallTypes": [
              {
                "sentenceType": "SENTENCE_TYPE",
                "recallType": {
                  "recallName": "RECALL_NAME",
                  "isStandardRecall": false,
                  "isFixedTermRecall": true
                }
              }
            ]
          }]
          """.trimMargin(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetSentenceAndRecallTypesWithStandardRecall(bookingId: Long = 123) {
    stubFor(
      post(urlEqualTo("/api/offender-sentences/bookings/sentence-and-recall-types")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          """[{
            "bookingId": "$bookingId",
            "sentenceTypeRecallTypes": [
              {
                "sentenceType": "SENTENCE_TYPE",
                "recallType": {
                  "recallName": "RECALL_NAME",
                  "isStandardRecall": true,
                  "isFixedTermRecall": false
                }
              }
            ]
          }]
          """.trimMargin(),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetSentenceAndRecallTypes(sentenceAndRecallTypes: List<BookingSentenceAndRecallTypes>) {
    stubFor(
      post(urlEqualTo("/api/offender-sentences/bookings/sentence-and-recall-types")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          mapper.writeValueAsString(sentenceAndRecallTypes),
        ).withStatus(200),
      ),
    )
  }

  fun stubGetSentencesAndOffences(
    bookingId: Long,
    sentencesAndOffences: List<OffenderSentenceAndOffences> = offenderSentencesAndOffences(bookingId),
  ) {
    stubFor(
      get(urlEqualTo("/api/offender-sentences/booking/$bookingId/sentences-and-offences")).willReturn(
        aResponse().withHeader("Content-Type", "application/json").withBody(
          mapper.writeValueAsString(sentencesAndOffences),
        ).withStatus(200),
      ),
    )
  }
}
