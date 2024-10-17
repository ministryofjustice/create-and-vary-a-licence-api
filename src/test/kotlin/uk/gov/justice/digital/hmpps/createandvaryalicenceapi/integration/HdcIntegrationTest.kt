package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

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

      assertThat(result.curfewTimes).isEqualTo(
        listOf(
          HdcCurfewTimes(
            1L,
            TestData.createHdcLicence(),
            1,
            DayOfWeek.MONDAY,
            LocalTime.of(19, 0),
            DayOfWeek.TUESDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
          HdcCurfewTimes(
            1L,
            TestData.createHdcLicence(),
            2,
            DayOfWeek.TUESDAY,
            LocalTime.of(19, 0),
            DayOfWeek.WEDNESDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
          HdcCurfewTimes(
            1L,
            TestData.createHdcLicence(),
            3,
            DayOfWeek.WEDNESDAY,
            LocalTime.of(19, 0),
            DayOfWeek.THURSDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
          HdcCurfewTimes(
            1L,
            TestData.createHdcLicence(),
            4,
            DayOfWeek.THURSDAY,
            LocalTime.of(19, 0),
            DayOfWeek.FRIDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
          HdcCurfewTimes(
            1L,
            TestData.createHdcLicence(),
            5,
            DayOfWeek.FRIDAY,
            LocalTime.of(19, 0),
            DayOfWeek.SATURDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
          HdcCurfewTimes(
            1L,
            TestData.createHdcLicence(),
            6,
            DayOfWeek.SATURDAY,
            LocalTime.of(19, 0),
            DayOfWeek.SUNDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
          HdcCurfewTimes (
            1L,
            TestData.createHdcLicence(),
            7,
            DayOfWeek.SUNDAY,
            LocalTime.of(19, 0),
            DayOfWeek.MONDAY,
            LocalTime.of(7, 0),
            LocalDateTime.of(2024, 8, 14, 9, 0),
          ),
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

    @Test
    fun `Service throws a 404 and not a 500 when a 404 is received from the HDC API`() {
      hdcApiMockServer.stubGetHdcLicenceDataNotFound(111111)
      val exception = webTestClient.get()
        .uri("/hdc/curfew/bookingId/111111")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(exception!!.status).isEqualTo(HttpStatus.NOT_FOUND.value())
      assertThat(exception.userMessage).isEqualTo("Not found: No licence data found for 111111")
      assertThat(exception.developerMessage).isEqualTo("No licence data found for 111111")
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

    val curfewTimes =
      listOf(
        HdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          1,
          DayOfWeek.MONDAY,
          LocalTime.of(20, 0),
          DayOfWeek.TUESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        HdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          2,
          DayOfWeek.TUESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.WEDNESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        HdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          3,
          DayOfWeek.WEDNESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.THURSDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        HdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          4,
          DayOfWeek.THURSDAY,
          LocalTime.of(20, 0),
          DayOfWeek.FRIDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        HdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          5,
          DayOfWeek.FRIDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SATURDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        HdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          6,
          DayOfWeek.SATURDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SUNDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        HdcCurfewTimes (
          1L,
          TestData.createHdcLicence(),
          7,
          DayOfWeek.SUNDAY,
          LocalTime.of(20, 0),
          DayOfWeek.MONDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
      )
  }
}
