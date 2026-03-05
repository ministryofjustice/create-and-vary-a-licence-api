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
  fun `Progress AP plus PSS licences job`() {
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

    assertThat(additionalConditionsAfter.none { it.conditionType == "PSS" }).isTrue
    assertThat(standardConditionsAfter.none { it.conditionType == "PSS" }).isTrue
  }

  private fun postRequest(
    uri: String,
    roles: List<String> = listOf("ROLE_CVL_ADMIN"),
  ): WebTestClient.ResponseSpec = webTestClient.post()
    .uri(uri)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = roles))
    .exchange()

  companion object {
    // Add shared mocks here if needed later
  }
}
