package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration

class DeactivateLicencesTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<DomainEventsService.HMPPSDomainEvent>()

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-deactivate-licences-past-release-date.sql",
  )
  fun `Given an in progress Licence past release date when POST run-deactivate-licences-past-release-date then licence should inactivated`() {
    webTestClient.post()
      .uri("/run-deactivate-licences-past-release-date")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    verify(eventsPublisher, times(3)).publishDomainEvent(eventCaptor.capture(), any())

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
        tuple(1L, LicenceStatus.INACTIVE),
        tuple(6L, LicenceStatus.INACTIVE),
        tuple(7L, LicenceStatus.INACTIVE),
      )
    assertThat(eventCaptor.allValues[0].eventType).isEqualTo(DomainEventsService.LicenceDomainEventType.LICENCE_INACTIVATED.value)
    assertThat(eventCaptor.allValues[1].eventType).isEqualTo(DomainEventsService.LicenceDomainEventType.LICENCE_INACTIVATED.value)
    assertThat(eventCaptor.allValues[2].eventType).isEqualTo(DomainEventsService.LicenceDomainEventType.LICENCE_INACTIVATED.value)
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
