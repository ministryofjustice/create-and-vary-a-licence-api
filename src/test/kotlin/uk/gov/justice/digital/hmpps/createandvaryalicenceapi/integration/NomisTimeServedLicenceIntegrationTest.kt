package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.RecordNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateNomisLicenceReasonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.NomisTimeServedLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.USER_EVENT

class NomisTimeServedLicenceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: NomisTimeServedLicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Test
  @Sql("classpath:test_data/seed-nomis-licence-id-1.sql")
  fun `should record NOMIS licence reason successfully`() {
    val request = RecordNomisLicenceReasonRequest(
      nomsId = "A1234BC",
      bookingId = 12345,
      reason = "Initial reason",
      prisonCode = "PRI",
    )

    webTestClient.post()
      .uri("/nomis-time-served-licence/record-reason")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val saved = licenceRepository.findByNomsIdAndBookingId("A1234BC", 12345).get()
    assertThat(saved.reason).isEqualTo("Initial reason")

    // Audit event exists
    val auditEvents = auditEventRepository.findAll()
    assertThat(auditEvents).isNotEmpty
    val auditEvent = auditEvents.last()
    assertThat(auditEvent.summary).isEqualTo("Recorded NOMIS licence reason")
    assertThat(auditEvent.changes).containsEntry("reason", "Initial reason")
    assertThat(auditEvent.username).isEqualTo("test-client")
  }

  @Test
  @Sql("classpath:test_data/seed-nomis-licence-id-1.sql")
  fun `should update NOMIS licence reason successfully`() {
    val updateRequest = UpdateNomisLicenceReasonRequest(
      reason = "Updated reason",
    )

    webTestClient.put()
      .uri("/nomis-time-served-licence/update-reason/A1234AA/123456")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus().isOk

    val updated = licenceRepository.findByNomsIdAndBookingId("A1234AA", 123456).get()
    assertThat(updated.reason).isEqualTo("Updated reason")

    // Audit event created
    val auditEvents = auditEventRepository.findAll()
    assertThat(auditEvents).isNotEmpty
    val auditEvent = auditEvents.last()

    assertThat(auditEvent.summary).isEqualTo("Updated NOMIS licence reason")
    assertThat(auditEvent.changes).containsEntry("reason (old)", "Time served licence created for conditional release")
    assertThat(auditEvent.changes).containsEntry("reason (new)", "Updated reason")
    assertThat(auditEvent.changes).containsEntry("nomsId", "A1234AA")
    assertThat(auditEvent.changes).containsEntry("bookingId", 123456)
    assertThat(auditEvent.eventType).isEqualTo(USER_EVENT)
    assertThat(auditEvent.username).isEqualTo("test-client")
  }

  @Test
  @Sql("classpath:test_data/seed-nomis-licence-id-1.sql")
  fun `should retrieve NOMIS licence reason successfully`() {
    webTestClient.get()
      .uri("/nomis-time-served-licence/reason/A1234AA/123456")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reason").isEqualTo("Time served licence created for conditional release")
  }

  @Test
  @Sql("classpath:test_data/seed-nomis-licence-id-1.sql")
  fun `should delete NOMIS licence reason successfully`() {
    // Verify the record exists before deletion
    val existing = licenceRepository.findByNomsIdAndBookingId("A1234AA", 123456).get()
    assertThat(existing.reason).isEqualTo("Time served licence created for conditional release")

    // Perform DELETE request
    webTestClient.delete()
      .uri("/nomis-time-served-licence/reason/A1234AA/123456")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // Verify the record is deleted
    val deleted = licenceRepository.findByNomsIdAndBookingId("A1234AA", 123456)
    assertThat(deleted).isEmpty

    // Verify audit event exists
    val auditEvents = auditEventRepository.findAll()
    assertThat(auditEvents).isNotEmpty
    val auditEvent = auditEvents.last()
    assertThat(auditEvent.summary).isEqualTo("Deleted NOMIS licence reason")
    assertThat(auditEvent.changes).containsEntry("reason (deleted)", "Time served licence created for conditional release")
    assertThat(auditEvent.username).isEqualTo("test-client")
  }

  @Test
  fun `should return OK and create audit when no NOMIS licence reason exists`() {
    // Ensure no record exists
    val missing = licenceRepository.findByNomsIdAndBookingId("NON_EXISTENT", 999999)
    assertThat(missing).isEmpty

    // Perform DELETE request
    webTestClient.delete()
      .uri("/nomis-time-served-licence/reason/NON_EXISTENT/999999")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // Verify still no record
    val stillMissing = licenceRepository.findByNomsIdAndBookingId("NON_EXISTENT", 999999)
    assertThat(stillMissing).isEmpty
  }

  @Test
  fun `should return 400 for invalid request body`() {
    val invalidRequest = RecordNomisLicenceReasonRequest(
      nomsId = "",
      bookingId = 0,
      reason = "",
      prisonCode = "",
    )

    webTestClient.post()
      .uri("/nomis-time-served-licence/record-reason")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(invalidRequest)
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `should return 403 for unauthorized role`() {
    val request = RecordNomisLicenceReasonRequest(
      nomsId = "A1234BC",
      bookingId = 12345,
      reason = "Test reason",
      prisonCode = "PRI",
    )

    webTestClient.post()
      .uri("/nomis-time-served-licence/record-reason")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `should return 404 when updating non-existent record`() {
    val updateRequest = UpdateNomisLicenceReasonRequest(
      reason = "Updated reason",
    )

    webTestClient.put()
      .uri("/nomis-time-served-licence/update-reason/A9999ZZ/99999")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus().isNotFound
  }
}
