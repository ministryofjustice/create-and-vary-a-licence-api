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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType.LICENCE_ACTIVATED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType.LICENCE_INACTIVATED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class SarLicenceActivationIntegrationTest : IntegrationTestBase() {
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
      .uri("/run-activation-job")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(7)).publishDomainEvent(capture())

      assertThat(allValues).extracting<Tuple> { tuple(it.eventType, it.additionalInformation?.licenceId) }
        .containsExactly(
          tuple(LICENCE_ACTIVATED.value, "1"),
          tuple(LICENCE_ACTIVATED.value, "2"),
          tuple(LICENCE_ACTIVATED.value, "3"),
          tuple(LICENCE_ACTIVATED.value, "7"),
          tuple(LICENCE_INACTIVATED.value, "4"),
          tuple(LICENCE_INACTIVATED.value, "5"),
          tuple(LICENCE_INACTIVATED.value, "6"),
        )
    }

    val activatedLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.ACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(activatedLicences?.size).isEqualTo(4)
    assertThat(activatedLicences)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(1L, LicenceStatus.ACTIVE),
        tuple(2L, LicenceStatus.ACTIVE),
        tuple(3L, LicenceStatus.ACTIVE),
        tuple(7L, LicenceStatus.ACTIVE),
      )

    val deactivatedLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.INACTIVE)))
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
        tuple(4L, LicenceStatus.INACTIVE),
        tuple(5L, LicenceStatus.INACTIVE),
        tuple(6L, LicenceStatus.INACTIVE),
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
