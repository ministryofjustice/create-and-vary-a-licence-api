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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE

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
        .containsExactly(
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

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      prisonerSearchMockServer.start()
      govUkMockServer.start()
      prisonerSearchMockServer.stubSearchPrisonersByBookingIds()
      prisonApiMockServer.stubGetCourtOutcomes()
      prisonApiMockServer.getHdcStatuses()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      prisonerSearchMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
