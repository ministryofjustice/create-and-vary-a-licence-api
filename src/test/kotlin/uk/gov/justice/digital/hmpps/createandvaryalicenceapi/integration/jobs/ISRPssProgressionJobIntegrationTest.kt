package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class ISRPssProgressionJobIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-isr-ap-pss-progression.sql",
  )
  fun `When progress of licence with type ofAP_PSS then type is updated and conditions are deleted as expected`() {
    // Given
    val uri = "/jobs/isr-ap-pss-progression"

    val licenceBefore = testRepository.findLicenceByTypeCode(LicenceType.AP_PSS)
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
    assertThat(auditEvent.summary).isEqualTo("Licence type automatically changed to AP for ISR PSS Progression due to PSS repeal")
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
