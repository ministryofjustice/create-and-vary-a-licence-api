package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class InactivateRecallLicencesIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-inactivate-recall-licences.sql",
  )
  fun `Run inactivate recall licences job`() {
    webTestClient.post()
      .uri("/run-inactivate-recall-licences-job")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val inactiveLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.INACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(inactiveLicences?.size).isEqualTo(6)

    assertThat(inactiveLicences)
      .extracting<Tuple> {
        Tuple.tuple(it.licenceId, it.licenceStatus)
      }
      .containsExactlyElementsOf(
        listOf(
          Tuple.tuple(1L, LicenceStatus.INACTIVE),
          Tuple.tuple(2L, LicenceStatus.INACTIVE),
          Tuple.tuple(4L, LicenceStatus.INACTIVE),
          Tuple.tuple(5L, LicenceStatus.INACTIVE),
          Tuple.tuple(6L, LicenceStatus.INACTIVE),
          Tuple.tuple(7L, LicenceStatus.INACTIVE),
        ),
      )

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(6)).publishDomainEvent(capture())

      assertThat(allValues)
        .extracting<Tuple> {
          Tuple.tuple(it.eventType, it.additionalInformation?.licenceId)
        }
        .containsExactly(
          Tuple.tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "1"),
          Tuple.tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "2"),
          Tuple.tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "4"),
          Tuple.tuple(LicenceDomainEventType.LICENCE_INACTIVATED.value, "6"),
          Tuple.tuple(LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value, "7"),
          Tuple.tuple(LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value, "5"),
        )
    }

    val remainingLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = LicenceStatus.IN_FLIGHT_LICENCES))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(remainingLicences?.size).isEqualTo(2)

    assertThat(remainingLicences)
      .extracting<Tuple> {
        Tuple.tuple(it.licenceId, it.licenceStatus)
      }
      .containsExactlyElementsOf(
        listOf(
          Tuple.tuple(3L, LicenceStatus.ACTIVE),
          Tuple.tuple(8L, LicenceStatus.IN_PROGRESS),
        ),
      )
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
