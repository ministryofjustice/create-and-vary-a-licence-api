package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class LicenceActivationIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-activation.sql",
  )
  fun `Run licence activation job`() {
    webTestClient.post()
      .uri("/run-activation-job")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(5)).publishDomainEvent(capture())
      val activationEvents = allValues.filter { it.eventType == LicenceDomainEventType.LICENCE_ACTIVATED.value }
      assertThat(activationEvents).hasSize(3)
      val inactivatedEvents = allValues.filter { it.eventType == LicenceDomainEventType.LICENCE_INACTIVATED.value }
      assertThat(inactivatedEvents).hasSize(2)
    }

    val activatedLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.ACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(activatedLicences?.size).isEqualTo(3)
    assertThat(activatedLicences)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(1L, LicenceStatus.ACTIVE),
        tuple(2L, LicenceStatus.ACTIVE),
        tuple(3L, LicenceStatus.ACTIVE),
      )

    val deactivatedLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.INACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(deactivatedLicences?.size).isEqualTo(2)
    assertThat(deactivatedLicences)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(4L, LicenceStatus.INACTIVE),
        tuple(5L, LicenceStatus.INACTIVE),
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
