package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class UpdateSentenceDatesIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var licenceEventRepository: LicenceEventRepository

  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update sentence dates`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(LocalDate.of(2024, 9, 8))

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          actualReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceEndDate = LocalDate.parse("2024-09-11"),
          licenceStartDate = LocalDate.parse("2023-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(LocalDate.parse("2024-09-08"))
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1.sql",
  )
  fun `Update sentence dates for HDC licence`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(LocalDate.of(2024, 9, 8))

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          actualReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceEndDate = LocalDate.parse("2024-09-11"),
          licenceStartDate = LocalDate.parse("2023-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
          homeDetentionCurfewActualDate = LocalDate.parse("2023-07-10"),
          homeDetentionCurfewEndDate = LocalDate.parse("2023-08-10"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(HdcLicence::class.java)
      .returnResult().responseBody

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(LocalDate.parse("2024-08-01"))
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
    assertThat(result?.homeDetentionCurfewActualDate).isEqualTo(LocalDate.parse("2023-07-10"))
    assertThat(result?.homeDetentionCurfewEndDate).isEqualTo(LocalDate.parse("2023-08-10"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-3.sql",
  )
  fun `Update sentence dates should set license status to inactive when the offender has a new future release date`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(LocalDate.of(2024, 9, 8))

    webTestClient.put()
      .uri("/licence/id/3/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.now().plusDays(5),
          actualReleaseDate = LocalDate.now().plusDays(2),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceEndDate = LocalDate.parse("2024-09-11"),
          licenceStartDate = LocalDate.parse("2024-09-08"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/3")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update sentence dates should set licence status to timed out when the licence is in hard stop period`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(LocalDate.now())

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.now(),
          actualReleaseDate = LocalDate.now(),
          sentenceStartDate = LocalDate.now().minusYears(2),
          sentenceEndDate = LocalDate.now().plusYears(1),
          licenceStartDate = LocalDate.now(),
          licenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionStartDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(LicenceStatus.TIMED_OUT)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update sentence dates should inactivate licence where licence was in hard stop period but is no longer in hard stop period`() {
    prisonApiMockServer.stubGetHdcLatest()
    prisonApiMockServer.stubGetCourtOutcomes()
    mockPrisonerSearchResponse(LocalDate.now())

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.now(),
          actualReleaseDate = LocalDate.now(),
          sentenceStartDate = LocalDate.now().minusYears(2),
          sentenceEndDate = LocalDate.now().plusYears(1),
          licenceStartDate = LocalDate.now(),
          licenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionStartDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val previousLicence = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(auditEventRepository.count()).isEqualTo(1)
    assertThat(licenceEventRepository.count()).isEqualTo(1)
    assertThat(previousLicence?.statusCode).isEqualTo(LicenceStatus.TIMED_OUT)

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.now().plusWeeks(1),
          actualReleaseDate = LocalDate.now().plusWeeks(1),
          sentenceStartDate = LocalDate.now().minusYears(2),
          sentenceEndDate = LocalDate.now().plusYears(1),
          licenceStartDate = LocalDate.now().plusWeeks(1),
          licenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionStartDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(2),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val currentLicence = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(1)).publishDomainEvent(capture())
      assertThat(allValues).hasSize(1)
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)
    }

    assertThat(auditEventRepository.count()).isEqualTo(3)
    assertThat(licenceEventRepository.count()).isEqualTo(2)
    assertThat(currentLicence?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
  }

  private fun mockPrisonerSearchResponse(releaseDate: LocalDate?) {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds(
      """[
            {
              "prisonerNumber": "A1234AA",
              "bookingId": "123",
              "status": "ACTIVE",
              "mostSeriousOffence": "Robbery",
              "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
              "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
              "homeDetentionCurfewEligibilityDate": null,
              "releaseDate": "$releaseDate",
              "confirmedReleaseDate": "$releaseDate",
              "conditionalReleaseDate": "$releaseDate",
              "paroleEligibilityDate": null,
              "actualParoleDate" : null,
              "postRecallReleaseDate": null,
              "homeDetentionCurfewActualDate": "2024-08-01",
              "legalStatus": "SENTENCED",
              "indeterminateSentence": false,
              "recall": false,
              "prisonId": "ABC",
              "bookNumber": "12345A",
              "firstName": "Test1",
              "lastName": "Person1",
              "dateOfBirth": "1985-01-01"
           }]
      """.trimIndent(),
    )
  }

  private companion object {
    val prisonApiMockServer = PrisonApiMockServer()
    val govUkApiMockServer = GovUkMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      govUkApiMockServer.start()
      prisonerSearchMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      govUkApiMockServer.stop()
      prisonerSearchMockServer.stop()
    }
  }
}
