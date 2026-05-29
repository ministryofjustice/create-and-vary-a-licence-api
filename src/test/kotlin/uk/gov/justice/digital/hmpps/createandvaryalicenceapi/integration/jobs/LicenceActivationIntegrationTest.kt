package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurrentPrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.createTestMapper
import java.time.LocalDate

class LicenceActivationIntegrationTest : IntegrationTestBase() {
  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-activation.sql",
  )
  fun `Run licence activation job`() {
    webTestClient.post()
      .uri("/jobs/activate-licences")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(9)).publishDomainEvent(capture())

      assertThat(allValues)
        .extracting<Tuple> { tuple(it.eventType, it.additionalInformation["licenceId"]) }
        .containsExactlyInAnyOrder(
          tuple(LicenceDomainEventType.LICENCE_ACTIVATED.value, "1"),
          tuple(LicenceDomainEventType.LICENCE_ACTIVATED.value, "2"),
          tuple(LicenceDomainEventType.LICENCE_ACTIVATED.value, "3"),
          tuple(LicenceDomainEventType.LICENCE_ACTIVATED.value, "7"),
          tuple(LicenceDomainEventType.HDC_LICENCE_ACTIVATED.value, "8"),
          tuple(LicenceDomainEventType.PRRD_LICENCE_ACTIVATED.value, "9"),
          tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "4"),
          tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "5"),
          tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "6"),
        )
    }

    val activatedLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(ACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(activatedLicences?.size).isEqualTo(6)
    assertThat(activatedLicences)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(1L, ACTIVE),
        tuple(2L, ACTIVE),
        tuple(3L, ACTIVE),
        tuple(7L, ACTIVE),
        tuple(8L, ACTIVE),
        tuple(9L, ACTIVE),
      )

    val deactivatedLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(INACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(deactivatedLicences?.size).isEqualTo(3)
    assertThat(deactivatedLicences)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(4L, INACTIVE),
        tuple(5L, INACTIVE),
        tuple(6L, INACTIVE),
      )
  }

  private companion object {
    val prisonApiMockServer = PrisonApiMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val govUkMockServer = GovUkMockServer()
    val hdcApiMockServer = HdcApiMockServer()

    private val mapper: ObjectMapper = createTestMapper()
    val mockPrisoners = mapper.writeValueAsString(
      listOf(
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AA",
          bookingId = "123",
          status = "INACTIVE",
          mostSeriousOffence = "Robbery",
          licenceExpiryDate = LocalDate.now().plusYears(1),
          sentenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(1),
          releaseDate = LocalDate.now().plusDays(1),
          confirmedReleaseDate = LocalDate.now(),
          conditionalReleaseDateOverrideDate = null,
          conditionalReleaseDate = LocalDate.now(),
          sentenceStartDate = LocalDate.now(),
          legalStatus = "SENTENCED",
          indeterminateSentence = false,
          recall = false,
          prisonId = "ABC",
          bookNumber = "12345A",
          firstName = "Test1",
          lastName = "Person1",
          dateOfBirth = LocalDate.parse("1985-01-01"),
          postRecallReleaseDate = null,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AB",
          bookingId = "456",
          status = "INACTIVE",
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
          postRecallReleaseDate = null,
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
          postRecallReleaseDate = null,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AD",
          bookingId = "012",
          status = "INACTIVE",
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
          firstName = "Test4",
          lastName = "Person4",
          dateOfBirth = LocalDate.parse("1987-01-01"),
          postRecallReleaseDate = null,
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AE",
          bookingId = "345",
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
          firstName = "Test5",
          lastName = "Person5",
          dateOfBirth = LocalDate.parse("1987-01-01"),
          postRecallReleaseDate = null,
          homeDetentionCurfewEligibilityDate = LocalDate.now(),
        ),
        PrisonerSearchPrisoner(
          prisonerNumber = "A1234AF",
          bookingId = "678",
          status = "INACTIVE",
          mostSeriousOffence = "Robbery",
          licenceExpiryDate = LocalDate.now().plusYears(1),
          sentenceExpiryDate = LocalDate.now().plusYears(1),
          topupSupervisionExpiryDate = LocalDate.now().plusYears(1),
          releaseDate = LocalDate.now().plusDays(1),
          confirmedReleaseDate = null,
          conditionalReleaseDate = null,
          legalStatus = "RECALL",
          indeterminateSentence = false,
          recall = false,
          prisonId = "GHI",
          bookNumber = "12345C",
          firstName = "Test6",
          lastName = "Person6",
          dateOfBirth = LocalDate.parse("1987-01-01"),
          postRecallReleaseDate = LocalDate.now(),
        ),
      ),
    )

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      prisonerSearchMockServer.start()
      govUkMockServer.start()
      hdcApiMockServer.start()
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds(mockPrisoners)
      prisonerSearchMockServer.stubSearchPrisonersByBookingIds(mockPrisoners)
      prisonApiMockServer.stubGetCourtOutcomes()
      hdcApiMockServer.stubGetHdcStatuses(
        listOf(
          CurrentPrisonerHdcStatus(345, HdcStatus.APPROVED),
        ),
      )
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      prisonerSearchMockServer.stop()
      govUkMockServer.stop()
      hdcApiMockServer.stop()
    }
  }
}
