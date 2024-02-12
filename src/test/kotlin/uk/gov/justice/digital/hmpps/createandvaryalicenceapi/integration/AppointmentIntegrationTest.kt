package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentWithType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AppointmentIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update person to meet at initial appointment`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentPerson")
      .bodyValue(anUpdateAppointmentPersonRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentWithType).isEqualTo(anUpdateAppointmentPersonRequest.appointmentWithType)
    assertThat(result?.appointmentPerson).isEqualTo(anUpdateAppointmentPersonRequest.appointmentPerson)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `update initial appointment time should throw validation error when Appointment time is null and Appointment type is SPECIFIC_DATE_TIME`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(anAppointmentTimeRequest.copy(appointmentTime = null))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `update initial appointment time should not throw validation error when Appointment time is null but Appointment type is not SPECIFIC_DATE_TIME`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(
        anAppointmentTimeRequest.copy(
          appointmentTime = null,
          appointmentTimeType = AppointmentTimeType.IMMEDIATE_UPON_RELEASE,
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentTime).isNull()
    assertThat(result?.appointmentTimeType)
      .isEqualTo(AppointmentTimeType.IMMEDIATE_UPON_RELEASE)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update time of the initial appointment`() {
    webTestClient.put()
      .uri("/licence/id/1/appointmentTime")
      .bodyValue(anAppointmentTimeRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentTime)
      .isEqualTo(anAppointmentTimeRequest.appointmentTime?.truncatedTo(ChronoUnit.MINUTES))
    assertThat(result?.appointmentTimeType)
      .isEqualTo(anAppointmentTimeRequest.appointmentTimeType)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the contact number for the officer on a licence`() {
    webTestClient.put()
      .uri("/licence/id/1/contact-number")
      .bodyValue(aContactNumberRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentContact).isEqualTo(aContactNumberRequest.telephone)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the address where the initial appointment will take place`() {
    webTestClient.put()
      .uri("/licence/id/1/appointment-address")
      .bodyValue(anAppointmentAddressRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.appointmentAddress).isEqualTo(anAppointmentAddressRequest.appointmentAddress)
  }

  private companion object {
    val anUpdateAppointmentPersonRequest = AppointmentPersonRequest(
      appointmentWithType = AppointmentWithType.DUTY_OFFICER,
      appointmentPerson = "John Smith",
    )

    val anAppointmentTimeRequest = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(10),
      appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
    )

    val aContactNumberRequest = ContactNumberRequest(
      telephone = "0114 2565555",
    )

    val anAppointmentAddressRequest = AppointmentAddressRequest(
      appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE",
    )

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
