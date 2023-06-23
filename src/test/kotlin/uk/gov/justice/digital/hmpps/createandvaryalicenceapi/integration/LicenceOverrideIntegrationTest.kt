package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.OverrideLicenceStatusRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class LicenceOverrideIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @Autowired
  lateinit var licenceEventRepository: AuditEventRepository

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.post()
      .uri("/licence/id/1/override/status")
      .bodyValue(
        OverrideLicenceStatusRequest(
          LicenceStatus.ACTIVE,
          "Override Test",
        ),
      )
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
  @Sql("classpath:test_data/seed-licence-id-1.sql")
  fun `Override licence with new status code`() {
    webTestClient.post()
      .uri("/licence/id/1/override/status")
      .bodyValue(
        OverrideLicenceStatusRequest(
          LicenceStatus.ACTIVE,
          "Override Test",
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus()
      .isAccepted

    val updatedLicence = licenceRepository.findById(1L).get()

    assertThat(LicenceStatus.ACTIVE).isEqualTo(updatedLicence.statusCode)
    assertThat(auditEventRepository.count()).isEqualTo(1)
    assertThat(licenceEventRepository.count()).isEqualTo(1)
  }

  @Test
  @Sql("classpath:test_data/seed-licence-id-1.sql")
  fun `Override licence fails when submitting status code already in use`() {
    webTestClient.post()
      .uri("/licence/id/1/override/status")
      .bodyValue(
        OverrideLicenceStatusRequest(
          LicenceStatus.IN_PROGRESS,
          "Override Test",
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  @Sql("classpath:test_data/seed-licence-id-1.sql")
  fun `Override licence dates`() {
    val initialLicence = licenceRepository.findById(1L).get()
    val newCrd = initialLicence.conditionalReleaseDate?.plusDays(1)
    val newSsd = initialLicence.sentenceStartDate?.minusMonths(1)
    val newTussd = initialLicence.topupSupervisionStartDate?.minusDays(5)

    webTestClient.put()
      .uri("/licence/id/1/override/dates")
      .bodyValue(
        OverrideLicenceDatesRequest(
          conditionalReleaseDate = newCrd,
          actualReleaseDate = initialLicence.actualReleaseDate,
          sentenceStartDate = newSsd,
          sentenceEndDate = initialLicence.sentenceEndDate,
          licenceStartDate = initialLicence.licenceStartDate,
          licenceExpiryDate = initialLicence.licenceExpiryDate,
          topupSupervisionExpiryDate = initialLicence.topupSupervisionExpiryDate,
          topupSupervisionStartDate = newTussd,
          reason = "Override dates test",
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus()
      .isAccepted

    val updatedLicence = licenceRepository.findById(1L).get()

    assertThat(updatedLicence.conditionalReleaseDate).isEqualTo(newCrd)
    assertThat(updatedLicence.actualReleaseDate).isEqualTo(initialLicence.actualReleaseDate)
    assertThat(updatedLicence.sentenceStartDate).isEqualTo(newSsd)
    assertThat(updatedLicence.sentenceEndDate).isEqualTo(initialLicence.sentenceEndDate)
    assertThat(updatedLicence.licenceStartDate).isEqualTo(initialLicence.licenceStartDate)
    assertThat(updatedLicence.licenceExpiryDate).isEqualTo(initialLicence.licenceExpiryDate)
    assertThat(updatedLicence.topupSupervisionExpiryDate).isEqualTo(initialLicence.topupSupervisionExpiryDate)
    assertThat(updatedLicence.topupSupervisionStartDate).isEqualTo(newTussd)
    assertThat(auditEventRepository.count()).isEqualTo(1)
    assertThat(licenceEventRepository.count()).isEqualTo(1)
  }
}
