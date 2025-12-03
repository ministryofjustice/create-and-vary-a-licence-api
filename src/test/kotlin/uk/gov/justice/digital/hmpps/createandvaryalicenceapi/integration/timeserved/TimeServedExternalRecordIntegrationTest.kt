package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.timeserved

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ExternalTimeServedRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.timeserved.TimeServedExternalRecordRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT

class TimeServedExternalRecordIntegrationTest : IntegrationTestBase() {
  @Nested
  inner class `Setting time served external record` {

    @Test
    fun `should record NOMIS licence reason successfully`() {
      // Given
      val roles = listOf("ROLE_CVL_ADMIN")
      val request = ExternalTimeServedRecordRequest(
        reason = "Initial reason",
        prisonCode = "PRI",
      )

      // When
      val response = webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = roles))
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()

      // Then
      response.expectStatus().isOk

      val saved = testRepository.findTimeServedExternalRecordByNomsIdAndBookingId("A1234AA", 123)
      assertThat(saved).isNotNull
      assertThat(saved!!.reason).isEqualTo("Initial reason")
      assertThat(saved.prisonCode).isEqualTo("PRI")
      assertThat(saved.nomsId).isEqualTo("A1234AA")
      assertThat(saved.bookingId).isEqualTo(123)
      assertThat(saved.bookingId).isEqualTo(123)
      assertThat(saved.updatedByCa).isNotNull
      assertThat(saved.updatedByCa.fullName).isEqualTo("Test Client")
      assertThat(saved.dateCreated).isNotNull
      assertThat(saved.dateLastUpdated).isNotNull

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
      // Given
      val roles = listOf("ROLE_CVL_ADMIN")
      val updateRequest = ExternalTimeServedRecordRequest(
        prisonCode = "LEI",
        reason = "Updated reason",
      )

      // When
      val response = webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = roles))
        .contentType(APPLICATION_JSON)
        .bodyValue(updateRequest)
        .exchange()

      // Then
      response.expectStatus().isOk

      val updated = testRepository.findTimeServedExternalRecordByNomsIdAndBookingId("A1234AA", 123)
      assertThat(updated?.reason).isEqualTo("Updated reason")

      val auditEvents = testRepository.findAllAuditEvents()
      assertThat(auditEvents).isNotEmpty
      val auditEvent = auditEvents.last()

      assertThat(auditEvent.summary).isEqualTo("TimeServed External Record Reason updated")
      assertThat(auditEvent.changes).containsEntry("reason (old)", "Time served licence created for conditional release")
      assertThat(auditEvent.changes).containsEntry("reason (new)", "Updated reason")
      assertThat(auditEvent.changes).containsEntry("nomsId", "A1234AA")
      assertThat(auditEvent.changes).containsEntry("bookingId", 123)
      assertThat(auditEvent.eventType).isEqualTo(USER_EVENT)
      assertThat(auditEvent.username).isEqualTo("test-client")
    }

    @Test
    fun `should return 400 for invalid request body`() {
      // Given
      val roles = listOf("ROLE_CVL_ADMIN")
      val invalidRequest = TimeServedExternalRecordRequest(
        nomsId = "",
        bookingId = 0,
        reason = "",
        prisonCode = "",
      )

      // When
      val response = webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = roles))
        .contentType(APPLICATION_JSON)
        .bodyValue(invalidRequest)
        .exchange()

      // Then
      response.expectStatus().isBadRequest
    }

    @Test
    fun `should return 403 for unauthorized role`() {
      // Given
      val wrongRoles = listOf("ROLE_CVL_WRONG")
      val request = TimeServedExternalRecordRequest(
        nomsId = "A1234AA",
        bookingId = 12345,
        reason = "Test reason",
        prisonCode = "PRI",
      )

      // When
      val response = webTestClient.put().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = wrongRoles))
        .contentType(APPLICATION_JSON)
        .bodyValue(request)
        .exchange()

      // Then
      response.expectStatus().isForbidden
    }
  }

  @Nested
  inner class `Retrieving time served external record` {

    @Test
    @Sql("classpath:test_data/seed-time-served-external-records-id-1.sql")
    fun `should retrieve NOMIS licence reason successfully`() {
      // Given
      val roles = listOf("ROLE_CVL_ADMIN")

      // When
      val response = webTestClient.get().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = roles))
        .exchange()

      // Then
      response.expectStatus().isOk
        .expectBody()
        .jsonPath("$.reason").isEqualTo("Time served licence created for conditional release")
    }

    @Test
    fun `should return 404 for missing NOMIS licence`() {
      // Given
      val roles = listOf("ROLE_CVL_ADMIN")

      // When
      val response = webTestClient.get().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = roles))
        .exchange()

      // Then
      response.expectStatus().isNotFound
    }

    @Test
    fun `should return 403 for unauthorized role`() {
      // Given
      val wrongRoles = listOf("ROLE_CVL_WRONG")

      // When
      val response = webTestClient.get().uri("/time-served/external-records/A1234AA/123")
        .headers(setAuthorisation(roles = wrongRoles))
        .exchange()

      // Then
      response.expectStatus().isForbidden
    }
  }
}
