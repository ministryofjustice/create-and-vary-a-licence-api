package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import kotlin.jvm.optionals.getOrNull

class ElectronicMonitoringProgrammeIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `should update electronic monitoring programme details successfully`() {
    val request = UpdateElectronicMonitoringProgrammeRequest(
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
  @Sql(
    "classpath:test_data/seed-prrd-licence-id-1.sql",
  )
  fun `should update electronic monitoring programme details successfully for PRRD licence`() {
    // Given
    val request = UpdateElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Test Programme",
    )

    // When
    val result = webTestClient.post()
      .uri("/licence/id/1/electronic-monitoring-programmes")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().isOk
    val licence = licenceRepository.findById(1L).getOrNull()
    assertThat(licence).isInstanceOf(PrrdLicence::class.java)
    val prrdLicence = licence as PrrdLicence
    assertThat(prrdLicence.electronicMonitoringProvider)
      .extracting("isToBeTaggedForProgramme", "programmeName")
      .containsAll(listOf(request.isToBeTaggedForProgramme, request.programmeName))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-prrd-ex-electronic-provider-licence-id-1.sql",
  )
  fun `when no provider for electronic monitoring PRRD licence then error thrown`() {
    // Given
    val request = UpdateElectronicMonitoringProgrammeRequest(
      isToBeTaggedForProgramme = true,
      programmeName = "Test Programme",
    )

    // When
    val result = webTestClient.post()
      .uri("/licence/id/1/electronic-monitoring-programmes")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()

    // Then
    result.expectStatus().is5xxServerError
    val errorResponse = getErrorResponse(result)
    assertThat(errorResponse.userMessage).contains("ElectronicMonitoringProvider is null for PrrdLicence: 1")
  }

  @Test
  fun `should return 400 for invalid request`() {
    val request = UpdateElectronicMonitoringProgrammeRequest(
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
    val request = UpdateElectronicMonitoringProgrammeRequest(
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
    val request = UpdateElectronicMonitoringProgrammeRequest(
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

  private fun getErrorResponse(
    result: WebTestClient.ResponseSpec,
    exceptedStatus: HttpStatusCode = HttpStatusCode.valueOf(500),
  ): ErrorResponse {
    result.expectStatus().isEqualTo(exceptedStatus)
    val errorResponse = result.expectBody(ErrorResponse::class.java).returnResult().responseBody
    assertThat(errorResponse).isNotNull
    return errorResponse!!
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
