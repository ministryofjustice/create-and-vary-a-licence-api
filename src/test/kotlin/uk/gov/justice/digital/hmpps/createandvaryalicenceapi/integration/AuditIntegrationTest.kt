package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime

class AuditIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.put()
      .uri("/audit/save")
      .bodyValue(anAuditEvent)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access is denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.put()
      .uri("/audit/save")
      .bodyValue(anAuditEvent)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/clear-audit-events.sql",
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Create an audit event`() {
    webTestClient.put()
      .uri("/audit/save")
      .bodyValue(anAuditEvent)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(auditEventRepository.count()).isEqualTo(1)
    assertThat(auditEventRepository.findAll())
      .extracting<Tuple> { tuple(it.licenceId, it.username, it.summary, it.detail) }
      .contains(tuple(1L, "USER", "Summary", "Detail"))
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-licence-id-1.sql",
    "classpath:test_data/clear-audit-events.sql",
    "classpath:test_data/seed-audit-events.sql"
  )
  fun `Get audit events for a licence and user`() {
    val result = webTestClient.post()
      .uri("/audit/retrieve")
      .bodyValue(aRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(AuditEvent::class.java)
      .returnResult().responseBody

    log.info("Response was ${mapper.writeValueAsString(result)}")

    assertThat(result).hasSize(3)
    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.username, it.summary, it.detail) }
      .contains(
        tuple(1L, "USER", "Summary1", "Detail1"),
        tuple(1L, "USER", "Summary2", "Detail2"),
        tuple(1L, "USER", "Summary3", "Detail3"),
      )
  }

  companion object {
    val anAuditEvent = AuditEvent(
      licenceId = 1L,
      eventTime = LocalDateTime.now(),
      username = "USER",
      fullName = "Forename Surname",
      eventType = AuditEventType.USER_EVENT,
      summary = "Summary",
      detail = "Detail",
    )

    val aRequest = AuditRequest(
      username = "USER",
      licenceId = 1L,
      startTime = LocalDateTime.now().minusMonths(1),
      endTime = LocalDateTime.now().plusMinutes(5),
    )
  }
}
