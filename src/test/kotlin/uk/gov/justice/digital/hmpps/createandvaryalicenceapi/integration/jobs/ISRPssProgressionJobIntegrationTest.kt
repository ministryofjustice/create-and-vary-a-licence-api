package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class ISRPssProgressionJobIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql("classpath:test_data/seed-isr-ap-pss-progression.sql")
  fun `should process all licences and update types statuses correctly`() {
    // Given
    val uri = "/jobs/isr-licence-progression-job"

    val inFlightApPssLicences = testRepository.findLicenceBy(LicenceStatus.IN_PROGRESS, LicenceType.AP_PSS)
    val activePssLicences = testRepository.findLicenceBy(LicenceStatus.ACTIVE, LicenceType.PSS)
    val activeApPssLicences = testRepository.findLicenceBy(LicenceStatus.ACTIVE, LicenceType.AP_PSS)

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    // Check in-flight AP_PSS licences converted to AP and PSS conditions removed
    inFlightApPssLicences.forEach { licence ->
      val updated = testRepository.findLicence(licence.id)
      assertThat(updated.typeCode).isEqualTo(LicenceType.AP)
      assertThat(updated.additionalConditions.none { it.conditionType == "PSS" }).isTrue
      assertThat(updated.standardConditions.none { it.conditionType == "PSS" }).isTrue
    }

    // Check active PSS licences set to INACTIVE
    activePssLicences.forEach { licence ->
      val updated = testRepository.findLicence(licence.id)
      assertThat(updated.statusCode.name).isEqualTo(LicenceStatus.INACTIVE.name)
    }

    // Check active AP_PSS licences converted to AP
    activeApPssLicences.forEach { licence ->
      val updated = testRepository.findLicence(licence.id)
      assertThat(updated.typeCode).isEqualTo(LicenceType.AP)
    }

    // Check audit events
    val events = testRepository.findAllAuditEvents().sortedBy { it.id }
    assertThat(events).size().isEqualTo(5)
    assertThat(events[0].summary).contains("ISR PSS licence changed to INACTIVE for Test3 Tester3 due to PSS repeal")
    assertThat(events[1].summary).contains("ISR AP_PSS licence to AP for Test1 Tester1 due to PSS repeal")
    assertThat(events[2].summary).contains("ISR AP licence for Test1 Tester1 has expired as the LED is in the past")
    assertThat(events[3].summary).contains("ISR AP_PSS licence to AP for Test2 Tester2 due to PSS repeal")
    assertThat(events[4].summary).contains("ISR AP licence for Test2 Tester2 has expired as the LED is in the past")
  }

  private fun postRequest(uri: String, roles: List<String> = listOf("ROLE_CVL_ADMIN")): WebTestClient.ResponseSpec = webTestClient.post()
    .uri(uri)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = roles))
    .exchange()

  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun overrideProperties(registry: DynamicPropertyRegistry) {
      registry.add("feature.toggle.isr.repeal.date") { LocalDate.now().toString() }
    }
  }
}
