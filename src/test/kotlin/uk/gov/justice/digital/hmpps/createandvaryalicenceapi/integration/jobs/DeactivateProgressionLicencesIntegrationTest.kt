package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType.LICENCE_INACTIVATED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import java.time.Duration

class DeactivateProgressionLicencesIntegrationTest : IntegrationTestBase() {
  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @MockitoBean
  lateinit var notifyService: NotifyService

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var licenceEventRepository: LicenceEventRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-progression-deactivation.sql",
  )
  fun `Job runs to deactivate HDC licences`() {
    webTestClient.post()
      .uri("/jobs/deactivate-progression-licences")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNoContent

    val deactivatedProgressionLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(INACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(deactivatedProgressionLicences.size).isEqualTo(8)
    verify(telemetryClient).trackEvent("DeactivateProgressionLicencesJob", mapOf("licences" to "8"), null)
    assertThat(deactivatedProgressionLicences)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        tuple(1L, INACTIVE),
        tuple(2L, INACTIVE),
        tuple(5L, INACTIVE),
        tuple(6L, INACTIVE),
        tuple(7L, INACTIVE),
        tuple(8L, INACTIVE),
        tuple(9L, INACTIVE),
        tuple(12L, INACTIVE),
      )

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(8)).publishDomainEvent(capture())
      assertThat(allValues)
        .allMatch { it.eventType == LICENCE_INACTIVATED.value }
    }

    val crnCaptor = argumentCaptor<String>()
    verify(notifyService, times(7)).sendLicenceDeactivatedForProgressionEmail(
      any(),
      crnCaptor.capture(),
      any(),
      any(),
      any(),
      any(),
    )

    assertThat(crnCaptor.allValues).containsExactlyInAnyOrder(
      "A123456",
      "A123456",
      "C345678",
      "D901234",
      "E567890",
      "F123456",
      "J567890",
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-progression-deactivation.sql",
  )
  fun `Job requires no roles`() {
    webTestClient.post()
      .uri("/jobs/deactivate-progression-licences")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNoContent

    verify(telemetryClient).trackEvent("DeactivateProgressionLicencesJob", mapOf("licences" to "8"), null)
  }
}
