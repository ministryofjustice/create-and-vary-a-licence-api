package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
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
                  "status": "INACTIVE",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "ABC",
                  "bookNumber": "12345A",
                  "firstName": "Test1",
                  "lastName": "Person1",
                  "dateOfBirth": "1985-01-01"
               },
               {
                  "prisonerNumber": "G5613GT",
                  "bookingId": "789",
                  "status": "INACTIVE",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "DEF",
                  "bookNumber": "67890B",
                  "firstName": "Test2",
                  "lastName": "Person2",
                  "dateOfBirth": "1986-01-01"
               },
               {
                  "prisonerNumber": "G4169UO",
                  "bookingId": "432",
                  "status": "INACTIVE",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
               },
               {
                  "prisonerNumber": "G7285AA",
                  "bookingId": "521",
                  "status": "INACTIVE",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test4",
                  "lastName": "Person4",
                  "dateOfBirth": "1987-01-01"
               }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }

  fun nextWorkingDate() = generateSequence(LocalDate.now()) { it.plusDays(1) }.filterNot { setOf(SATURDAY, SUNDAY).contains(it.dayOfWeek) }.first()

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
                  "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now().plusDays(1)}",
                  "confirmedReleaseDate": "${nextWorkingDate()}",
                  "conditionalReleaseDate": "${nextWorkingDate()}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "ABC",
                  "bookNumber": "12345A",
                  "firstName": "Test1",
                  "lastName": "Person1",
                  "dateOfBirth": "1985-01-01"
               },
               {
                  "prisonerNumber": "A1234AB",
                  "bookingId": "456",
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "DEF",
                  "bookNumber": "67890B",
                  "firstName": "Test2",
                  "lastName": "Person2",
                  "dateOfBirth": "1986-01-01"
               },
               {
                  "prisonerNumber": "A1234AC",
                  "bookingId": "789",
                  "status": "INACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": null,
                  "topupSupervisionExpiryDate": null,
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": null,
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
               },
               {
                  "prisonerNumber": "A1234AD",
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
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
               },
               {
                  "prisonerNumber": "A1234AE",
                  "bookingId": "123",
                  "status": "INACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().minusYears(1)}",
                  "topUpSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now().minusYears(1)}",
                  "confirmedReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "conditionalReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
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

  fun stubSearchPrisonersByReleaseDate() {
    stubFor(
      post(urlEqualTo("/api/prisoner-search/release-date-by-prison?size=2000"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """{ "content": [
                {
                  "prisonerNumber": "A1234AA",
                  "bookingId": "123",
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now().plusDays(1)}",
                  "confirmedReleaseDate": "${nextWorkingDate()}",
                  "conditionalReleaseDate": "${nextWorkingDate()}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "ABC",
                  "bookNumber": "12345A",
                  "firstName": "Test1",
                  "lastName": "Person1",
                  "dateOfBirth": "1985-01-01"
               },
               {
                  "prisonerNumber": "A1234AB",
                  "bookingId": "456",
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "DEF",
                  "bookNumber": "67890B",
                  "firstName": "Test2",
                  "lastName": "Person2",
                  "dateOfBirth": "1986-01-01"
               },
               {
                  "prisonerNumber": "A1234AC",
                  "bookingId": "789",
                  "status": "INACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": null,
                  "topupSupervisionExpiryDate": null,
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": null,
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": null,
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
               },
               {
                  "prisonerNumber": "A1234AD",
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
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
               },
               {
                  "prisonerNumber": "A1234AE",
                  "bookingId": "123",
                  "status": "INACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().minusYears(1)}",
                  "topUpSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now().minusYears(1)}",
                  "confirmedReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "conditionalReleaseDate": "${LocalDate.now().plusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test3",
                  "lastName": "Person3",
                  "dateOfBirth": "1987-01-01"
               }
              ]}
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }
}
