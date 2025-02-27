package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions
import org.assertj.core.groups.Tuple
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration

class DeactivateHdcLicencesIntegrationTest : IntegrationTestBase() {
  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

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
    "classpath:test_data/seed-licences-for-deactivate-hdc-licences-past-release-date.sql",
  )
  fun `Job runs to deactivate HDC licences`() {
    webTestClient.post()
      .uri("/jobs/deactivate-hdc-licences")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNoContent

    val deactivatedHdcLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.INACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    Assertions.assertThat(deactivatedHdcLicences?.size).isEqualTo(3)
    verify(telemetryClient).trackEvent("DeactivateHdcLicencesJob", mapOf("licences" to "3"), null)
    Assertions.assertThat(deactivatedHdcLicences)
      .extracting<Tuple> {
        Tuple.tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        Tuple.tuple(1L, LicenceStatus.INACTIVE),
        Tuple.tuple(2L, LicenceStatus.INACTIVE),
        Tuple.tuple(4L, LicenceStatus.INACTIVE),
      )

    argumentCaptor<DomainEventsService.HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(3)).publishDomainEvent(capture())
      Assertions.assertThat(allValues)
        .allMatch { it.eventType == DomainEventsService.LicenceDomainEventType.HDC_LICENCE_INACTIVATED.value }
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-deactivate-hdc-licences-past-release-date.sql",
  )
  fun `Job requires no roles`() {
    webTestClient.post()
      .uri("/jobs/deactivate-hdc-licences")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNoContent

    verify(telemetryClient).trackEvent("DeactivateHdcLicencesJob", mapOf("licences" to "3"), null)
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
