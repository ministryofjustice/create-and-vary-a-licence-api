package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewHours
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData

class HdcIntegrationTest : IntegrationTestBase() {

  @Nested
  inner class GetHdcLicenceData {

    @Test
    fun `Get HDC licence data by booking ID`() {
      hdcApiMockServer.stubGetHdcLicenceData(12345L)

      val result = webTestClient.get()
        .uri("/hdc/curfew/bookingId/12345")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(HdcLicenceData::class.java)
        .returnResult().responseBody

      assertThat(result!!.curfewAddress).isEqualTo(
        CurfewAddress(
          "123 Test Street",
          null,
          "Test Area",
          "AB1 2CD",
        ),
      )
      assertThat(result.firstNightCurfewHours).isEqualTo(
        FirstNight(
          "15:00",
          "07:00",
        ),
      )
      assertThat(result.curfewHours).isEqualTo(
        CurfewHours(
          "19:00",
          "07:00",
          "19:00",
          "07:00",
          "19:00",
          "07:00",
          "19:00",
          "07:00",
          "19:00",
          "07:00",
          "19:00",
          "07:00",
          "19:00",
          "07:00",
        ),
      )
    }

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.get()
        .uri("/hdc/curfew/bookingId/12345")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.get()
        .uri("/hdc/curfew/bookingId/12345")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
    }
  }

  private companion object {
    val hdcApiMockServer = HdcApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      hdcApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      hdcApiMockServer.stop()
    }
  }
}
