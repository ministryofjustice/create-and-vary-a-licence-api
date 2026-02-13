package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate

class PrisonerSearchMockServer : WireMockServer(8099) {

  private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    .registerKotlinModule()

  fun stubSearchPrisonersByBookingIds(nomisId: String = "G7285UT") {
    stubFor(
      post(urlEqualTo("/api/prisoner-search/booking-ids"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            """[
                {
                  "prisonerNumber": "$nomisId",
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
               },
               {
                  "prisonerNumber": "G1234BB",
                  "bookingId": "123",
                  "status": "INACTIVE",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "54321D",
                  "firstName": "Test5",
                  "lastName": "Person5",
                  "dateOfBirth": "1988-01-01",
                  "homeDetentionCurfewEligibilityDate": "${LocalDate.now().plusDays(1)}"
              },
              {
                  "prisonerNumber": "G6397ZX",
                  "bookingId": "635",
                  "status": "INACTIVE",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": true,
                  "prisonId": "ABC",
                  "bookNumber": "76329K",
                  "firstName": "Test6",
                  "lastName": "Person6",
                  "dateOfBirth": "1962-01-01"
               }
              ]
            """.trimIndent(),
          ).withStatus(200),
        ),
    )
  }

  fun nextWorkingDate() = nextWorkingDates().first()

  fun nextWorkingDates(): Sequence<LocalDate> = generateSequence(LocalDate.now()) { it.plusDays(1) }.filterNot { setOf(SATURDAY, SUNDAY).contains(it.dayOfWeek) }

  fun stubSearchPrisonersByNomisIds(
    prisonerSearchResponse: String? = null,
    postRecallReleaseDate: LocalDate? = null,
    prisonId: String = "ABC",
    confirmedReleaseDate: LocalDate? = null,
    sentenceStartDate: LocalDate? = null,
    conditionalReleaseDate: LocalDate? = null,
    conditionalReleaseDateOverrideDate: LocalDate? = null,
  ) {
    val jsonString: String
    if (prisonerSearchResponse == null) {
      val prisoners = listOf(
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AA",
          bookingId = "123",
          status = "ACTIVE",
          mostSeriousOffence = "Robbery",
          licenceExpiryDate = LocalDate.now().plusYears(1),
          sentenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(1),
          releaseDate = LocalDate.now().plusDays(1),
          confirmedReleaseDate = confirmedReleaseDate ?: nextWorkingDate(),
          conditionalReleaseDateOverrideDate = conditionalReleaseDateOverrideDate,
          conditionalReleaseDate = conditionalReleaseDate ?: nextWorkingDate(),
          sentenceStartDate = sentenceStartDate ?: nextWorkingDate(),
          legalStatus = "SENTENCED",
          indeterminateSentence = false,
          recall = false,
          prisonId = prisonId,
          bookNumber = "12345A",
          firstName = "Test1",
          lastName = "Person1",
          dateOfBirth = LocalDate.parse("1985-01-01"),
          postRecallReleaseDate = postRecallReleaseDate,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AB",
          bookingId = "456",
          status = "ACTIVE",
          mostSeriousOffence = "Robbery",
          licenceExpiryDate = LocalDate.now().plusYears(1),
          sentenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(1),
          conditionalReleaseDate = LocalDate.now().plusDays(1),
          legalStatus = "SENTENCED",
          indeterminateSentence = false,
          recall = false,
          prisonId = "DEF",
          bookNumber = "67890B",
          firstName = "Test2",
          lastName = "Person2",
          dateOfBirth = LocalDate.parse("1986-01-01"),
          postRecallReleaseDate = postRecallReleaseDate,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AC",
          bookingId = "789",
          status = "INACTIVE",
          mostSeriousOffence = "Robbery",
          legalStatus = "SENTENCED",
          indeterminateSentence = false,
          recall = false,
          prisonId = "GHI",
          bookNumber = "12345C",
          firstName = "Test3",
          lastName = "Person3",
          dateOfBirth = LocalDate.parse("1987-01-01"),
          postRecallReleaseDate = postRecallReleaseDate,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AD",
          bookingId = "123",
          status = "ACTIVE",
          mostSeriousOffence = "Robbery",
          licenceExpiryDate = LocalDate.now().plusYears(1),
          sentenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(1),
          releaseDate = LocalDate.now().plusDays(1),
          confirmedReleaseDate = LocalDate.now().plusDays(1),
          conditionalReleaseDate = LocalDate.now().plusDays(1),
          legalStatus = "SENTENCED",
          indeterminateSentence = false,
          recall = false,
          prisonId = "GHI",
          bookNumber = "12345C",
          firstName = "Test3",
          lastName = "Person3",
          dateOfBirth = LocalDate.parse("1987-01-01"),
          postRecallReleaseDate = postRecallReleaseDate,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AE",
          bookingId = "123",
          status = "INACTIVE",
          mostSeriousOffence = "Robbery",
          licenceExpiryDate = LocalDate.now().minusYears(1),
          sentenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(1),
          releaseDate = LocalDate.now().minusYears(1),
          confirmedReleaseDate = LocalDate.now().plusDays(1),
          conditionalReleaseDate = LocalDate.now().plusDays(1),
          legalStatus = "SENTENCED",
          indeterminateSentence = false,
          recall = false,
          prisonId = "GHI",
          bookNumber = "12345C",
          firstName = "Test3",
          lastName = "Person3",
          dateOfBirth = LocalDate.parse("1987-01-01"),
          postRecallReleaseDate = postRecallReleaseDate,
        ),
      )
      jsonString = objectMapper.writeValueAsString(prisoners)
    } else {
      jsonString = prisonerSearchResponse
    }

    stubFor(
      post(urlEqualTo("/api/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            jsonString,
          ).withStatus(200),
        ),
    )
  }

  fun stubSearchPrisonersByNomisIdsHDCAPResult(prisonerSearchResponse: String? = null) {
    val json = prisonerSearchResponse ?: """[
            {
              "prisonerNumber": "A1234AA",
              "bookingId": "123",
              "status": "ACTIVE",
              "mostSeriousOffence": "Robbery",
              "licenceExpiryDate": null,
              "topupSupervisionExpiryDate": null,
              "homeDetentionCurfewEligibilityDate": "${LocalDate.now().plusDays(1)}",
              "homeDetentionCurfewActualDate": "${LocalDate.now().plusDays(1)}",
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
           }         
          ]
    """.trimIndent()

    stubFor(
      post(urlEqualTo("/api/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            json,
          ).withStatus(200),
        ),
    )
  }

  fun stubSearchPrisonersByNomisIdsHDCAPPSSResult(prisonerSearchResponse: String? = null) {
    val json = prisonerSearchResponse ?: """[
            {
              "prisonerNumber": "A1234AA",
              "bookingId": "123",
              "status": "ACTIVE",
              "mostSeriousOffence": "Robbery",
              "licenceExpiryDate": "${LocalDate.now()}",
              "topupSupervisionExpiryDate": "${LocalDate.now().plusDays(1)}",
              "homeDetentionCurfewActualDate": "${LocalDate.now().plusDays(1)}",
              "homeDetentionCurfewEligibilityDate": "${LocalDate.now().plusDays(1)}",
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
           }         
          ]
    """.trimIndent()

    stubFor(
      post(urlEqualTo("/api/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            json,
          ).withStatus(200),
        ),
    )
  }

  fun stubSearchPrisonersByNomisIdsHDCPSSResult(prisonerSearchResponse: String? = null) {
    val json = prisonerSearchResponse ?: """[
            {
              "prisonerNumber": "A1234AA",
              "bookingId": "123",
              "status": "ACTIVE",
              "mostSeriousOffence": "Robbery",
              "licenceExpiryDate": null,
              "topupSupervisionExpiryDate": "${LocalDate.now().plusDays(1)}",
              "homeDetentionCurfewEligibilityDate": "${LocalDate.now().plusDays(1)}",
              "homeDetentionCurfewActualDate": "${LocalDate.now().plusDays(1)}",
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
           }         
          ]
    """.trimIndent()

    stubFor(
      post(urlEqualTo("/api/prisoner-search/prisoner-numbers"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            json,
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

  fun stubSearchPrisonersByReleaseDate(page: Int, inHardStop: Boolean = true, includeRecall: Boolean = false) {
    val releaseDate = if (inHardStop) LocalDate.now().plusDays(1) else nextWorkingDates().drop(4).first()
    var jsonBody = """{ "content": [
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
                  "conditionalReleaseDate": "$releaseDate",
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
               },
               {
                  "prisonerNumber": "A1234AG",
                  "bookingId": "123",
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topUpSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now()}",
                  "sentenceStartDate": "${LocalDate.now()}",
                  "confirmedReleaseDate": "${LocalDate.now()}",
                  "conditionalReleaseDate": "${LocalDate.now()}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": null,
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "GHI",
                  "bookNumber": "12345C",
                  "firstName": "Test5",
                  "lastName": "Person5",
                  "dateOfBirth": "1987-01-01"
               }
               """
    if (includeRecall) {
      jsonBody += """    
               ,{
                  "prisonerNumber": "A1234AF",
                  "bookingId": "124",
                  "status": "ACTIVE",
                  "mostSeriousOffence": "Robbery",
                  "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
                  "topUpSupervisionExpiryDate": null,
                  "homeDetentionCurfewEligibilityDate": null,
                  "releaseDate": "${LocalDate.now().plusDays(1)}",
                  "confirmedReleaseDate": null,
                  "conditionalReleaseDate": "${releaseDate.minusDays(1)}",
                  "paroleEligibilityDate": null,
                  "actualParoleDate" : null,
                  "postRecallReleaseDate": "$releaseDate",
                  "legalStatus": "SENTENCED",
                  "indeterminateSentence": false,
                  "recall": false,
                  "prisonId": "ABC",
                  "bookNumber": "12345C",
                  "firstName": "Test4",
                  "lastName": "Person4",
                  "dateOfBirth": "1987-01-01"
               }
               """
    }
    jsonBody += """
              ],
              "pageable": {
                  "pageSize": 100,
                  "offset": 0,
                  "sort": {
                      "empty": false,
                      "unsorted": false,
                      "sorted": true
                  },
                  "pageNumber": 0,
                  "paged": true,
                  "unpaged": false
              },
              "totalElements": 5,
              "totalPages": 1,
              "last": true,
              "size": 2000,
              "number": 0,
              "sort": {
                  "empty": false,
                  "unsorted": false,
                  "sorted": true
              },
              "first": true,
              "numberOfElements": 5,
              "empty": false
            }
    """.trimIndent()
    stubSearchPrisonersByReleaseDate(jsonBody, page)
  }

  fun stubSearchPrisonersByReleaseDate(prisoners: List<PrisonerSearchPrisoner>) {
    val pageable: Pageable = PageRequest.of(0, 100, Sort.by("releaseDate").ascending())
    val prisonerPage: Page<PrisonerSearchPrisoner> = PageImpl(prisoners, pageable, prisoners.size.toLong())
    stubSearchPrisonersByReleaseDate(objectMapper.writeValueAsString(prisonerPage), 0)
  }

  fun stubSearchPrisonersByReleaseDate(jsonBody: String, page: Int) {
    stubFor(
      post(urlEqualTo("/api/prisoner-search/release-date-by-prison?size=2000&page=$page"))
        .willReturn(
          aResponse().withHeader(
            "Content-Type",
            "application/json",
          ).withBody(
            jsonBody.trimIndent(),
          ).withStatus(200),
        ),
    )
  }
}
