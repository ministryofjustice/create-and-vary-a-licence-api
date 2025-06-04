package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository

class ElectronicMonitoringProgrammeIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `should update electronic monitoring programme details successfully`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Test Programme",
    )

    webTestClient.post()
      .uri("/licence/id/1/electronic-monitoring-programmes")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk

    val result = licenceRepository.findById(1L).get() as CrdLicence

    assertThat(result.electronicMonitoringProvider)
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsAll(listOf(request.isToBeTaggedForProgramme, request.programmeName))
  }

  @Test
  fun `should return 400 for invalid request`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = null,
    )

    webTestClient.post()
      .uri("/licence/id/1/electronic-monitoring-programmes")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `should return 403 for unauthorized role`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Test Programme",
    )

    webTestClient.post()
      .uri("/licence/id/1/electronic-monitoring-programmes")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `should return 404 for non-existent licence ID`() {
    val request = ElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Test Programme",
    )

    webTestClient.post()
      .uri("/licence/id/999/electronic-monitoring-programmes")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isNotFound
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
