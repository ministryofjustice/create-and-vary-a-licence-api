package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ExternalTimeServedRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.TimeServedExternalRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT

class TimeServedExternalRecordIntegrationTest : IntegrationTestBase() {
  @Nested
  inner class `Setting time served external record` {
    @Test
    fun `should record NOMIS licence reason successfully`() {
      val request = ExternalTimeServedRecordRequest(
        reason = "Initial reason",
        prisonCode = "PRI",
      )

      webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).contentType(APPLICATION_JSON)
        .bodyValue(request).exchange().expectStatus().isOk

      val saved = testRepository.findTimeServedExternalRecordByNomsIdAndBookingId("A1234AA", 123)
      assertThat(saved?.reason).isEqualTo("Initial reason")

      // Audit event exists
      val auditEvents = testRepository.findAllAuditEvents()
      assertThat(auditEvents).isNotEmpty
      val auditEvent = auditEvents.last()
      assertThat(auditEvent.summary).isEqualTo("TimeServed External Record Reason created")
      assertThat(auditEvent.changes).containsEntry("reason", "Initial reason")
      assertThat(auditEvent.username).isEqualTo("test-client")
    }

    @Test
    @Sql("classpath:test_data/seed-time-served-external-records-id-1.sql")
    fun `should update NOMIS licence reason successfully`() {
      val updateRequest = ExternalTimeServedRecordRequest(
        prisonCode = "LEI",
        reason = "Updated reason",
      )

      webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).contentType(APPLICATION_JSON)
        .bodyValue(updateRequest).exchange().expectStatus().isOk

      val updated = testRepository.findTimeServedExternalRecordByNomsIdAndBookingId("A1234AA", 123)
      assertThat(updated?.reason).isEqualTo("Updated reason")

      // Audit event created
      val auditEvents = testRepository.findAllAuditEvents()
      assertThat(auditEvents).isNotEmpty
      val auditEvent = auditEvents.last()

      assertThat(auditEvent.summary).isEqualTo("TimeServed External Record Reason updated")
      assertThat(auditEvent.changes).containsEntry(
        "reason (old)",
        "Time served licence created for conditional release",
      )
      assertThat(auditEvent.changes).containsEntry("reason (new)", "Updated reason")
      assertThat(auditEvent.changes).containsEntry("nomsId", "A1234AA")
      assertThat(auditEvent.changes).containsEntry("bookingId", 123)
      assertThat(auditEvent.eventType).isEqualTo(USER_EVENT)
      assertThat(auditEvent.username).isEqualTo("test-client")
    }

    @Test
    fun `should return 400 for invalid request body`() {
      val invalidRequest = TimeServedExternalRecordRequest(
        nomsId = "",
        bookingId = 0,
        reason = "",
        prisonCode = "",
      )

      webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).contentType(APPLICATION_JSON)
        .bodyValue(invalidRequest).exchange().expectStatus().isBadRequest
    }

    @Test
    fun `should return 403 for unauthorized role`() {
      val request = TimeServedExternalRecordRequest(
        nomsId = "A1234AA",
        bookingId = 12345,
        reason = "Test reason",
        prisonCode = "PRI",
      )

      webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG"))).contentType(APPLICATION_JSON)
        .bodyValue(request).exchange().expectStatus().isForbidden
    }
  }

  @Nested
  inner class `Retrieving time served external record` {
    @Test
    @Sql("classpath:test_data/seed-time-served-external-records-id-1.sql")
    fun `should retrieve NOMIS licence reason successfully`() {
      webTestClient.get().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange().expectStatus().isOk.expectBody()
        .jsonPath("$.reason").isEqualTo("Time served licence created for conditional release")
    }

    @Test
    fun `should return 404 for missing NOMIS licence`() {
      webTestClient.get().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN"))).exchange().expectStatus().isNotFound
    }

    @Test
    fun `should return 403 for unauthorized role`() {
      webTestClient.get().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG")))
        .exchange().expectStatus().isForbidden
    }
  }
}
