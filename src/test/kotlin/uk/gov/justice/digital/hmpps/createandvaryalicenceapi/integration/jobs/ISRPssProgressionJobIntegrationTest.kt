package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class ISRPssProgressionJobIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-isr-ap-pss-progression.sql",
  )
  fun `when progress of licence with type of AP_PSS then type is updated and conditions are deleted as expected`() {
    // Given
    val uri = "/jobs/isr-in-flight-ap-pss-licences"

    val licenceBefore = testRepository.findLicenceBy(LicenceStatus.IN_PROGRESS, LicenceType.AP_PSS)
    val licenceId = licenceBefore.first().id

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    val licenceAfter = testRepository.findLicence(licenceId)
    assertThat(licenceAfter.typeCode).isEqualTo(LicenceType.AP)

    val additionalConditionsAfter = licenceAfter.additionalConditions.toList()
    val standardConditionsAfter = licenceAfter.standardConditions.toList()
    assertThat(additionalConditionsAfter.size).isEqualTo(1)
    assertThat(standardConditionsAfter.size).isEqualTo(1)
    assertThat(additionalConditionsAfter.none { it.conditionType == "PSS" }).isTrue
    assertThat(standardConditionsAfter.none { it.conditionType == "PSS" }).isTrue

    val auditEvent = testRepository.findAllAuditEvents().first()
    assertThat(auditEvent.summary).isEqualTo("In flight licence type automatically changed to AP for Test1 Tester1 due to PSS repeal")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-isr-ap-pss-progression-in-flight-statues.sql",
  )
  fun `when progress of licences with type of AP_PSS then all inflight status will be processed`() {
    // Given
    val uri = "/jobs/isr-in-flight-ap-pss-licences"

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk
    assertThat(testRepository.findAllLicence().count { it.typeCode == LicenceType.AP }).isEqualTo(3)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-isr-ap-pss-progression.sql",
  )
  fun `when progress active AP_PSS licence then licence will be converted to AP `() {
    // Given
    val uri = "/jobs/isr-active-licences"

    val licenceBefore = testRepository.findLicenceBy(LicenceStatus.ACTIVE, LicenceType.AP_PSS)
    val licenceId = licenceBefore.first().id

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    val licenceAfter = testRepository.findLicence(licenceId)
    assertThat(licenceAfter.typeCode).isEqualTo(LicenceType.AP)
    val auditEvent = testRepository.findAllAuditEvents().first()

    assertThat(auditEvent.summary)
      .isEqualTo("Active licence type automatically changed to INACTIVE for Test3 Tester3 due to PSS repeal")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-isr-ap-pss-progression.sql",
  )
  fun `when progress active PSS licence be set INACTIVE`() {
    // Given
    val uri = "/jobs/isr-active-licences"

    val licenceBefore = testRepository.findLicenceBy(LicenceStatus.ACTIVE, LicenceType.PSS)
    val licenceId = licenceBefore.first().id

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    val licenceAfter = testRepository.findLicence(licenceId)
    assertThat(licenceAfter.statusCode.name).isEqualTo("INACTIVE")

    val auditEvent = testRepository.findAllAuditEvents().first()
    assertThat(auditEvent.summary).isEqualTo("Active licence type automatically changed to INACTIVE for Test3 Tester3 due to PSS repeal")
  }

  private fun postRequest(
    uri: String,
    roles: List<String> = listOf("ROLE_CVL_ADMIN"),
  ): WebTestClient.ResponseSpec = webTestClient.post()
    .uri(uri)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = roles))
    .exchange()
}
