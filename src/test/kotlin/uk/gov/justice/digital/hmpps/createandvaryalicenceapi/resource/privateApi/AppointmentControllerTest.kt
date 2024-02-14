package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.ValidationException
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AppointmentService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@WebMvcTest(controllers = [AppointmentController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [AppointmentController::class])
@WebAppConfiguration
class AppointmentControllerTest {

  @MockBean
  private lateinit var appointmentService: AppointmentService

  @Autowired
  private lateinit var mvc: MockMvc

  @Autowired
  private lateinit var mapper: ObjectMapper

  @BeforeEach
  fun reset() {
    reset(appointmentService)

    mvc = MockMvcBuilders
      .standaloneSetup(AppointmentController(appointmentService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `update initial appointment person with type DUTY_OFFICER`() {
    mvc.perform(
      put("/licence/id/4/appointmentPerson")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anUpdateAppointmentPersonRequest)),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateAppointmentPerson(4, anUpdateAppointmentPersonRequest)
  }

  @Test
  fun `update initial appointment person - invalid request body`() {
    val anNullUpdateAppointmentPersonRequest = anUpdateAppointmentPersonRequest.copy(
      appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
      appointmentPerson = null,
    )
    whenever(
      appointmentService.updateAppointmentPerson(
        4,
        anNullUpdateAppointmentPersonRequest,
      ),
    ).thenThrow(ValidationException("Appointment person must not be null if Appointment With Type is SPECIFIC_PERSON"))

    val result = mvc.perform(
      put("/licence/id/4/appointmentPerson")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(anNullUpdateAppointmentPersonRequest),
        ),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("Appointment person must not be null if Appointment With Type is SPECIFIC_PERSON")
  }

  @Test
  fun `update initial appointment person with default type SPECIFIC_PERSON`() {
    mvc.perform(
      put("/licence/id/4/appointmentPerson")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(
          mapper.writeValueAsBytes(
            anUpdateAppointmentPersonRequest.copy(
              appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
            ),
          ),
        ),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateAppointmentPerson(
      4,
      anUpdateAppointmentPersonRequest.copy(
        appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
      ),
    )
  }

  @Test
  fun `update initial appointment time`() {
    mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentTimeRequest)),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateAppointmentTime(4, anAppointmentTimeRequest)
  }

  @Test
  fun `update initial appointment time - lower precision datetime`() {
    mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentTimeRequestDateOnly)),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateAppointmentTime(4, anAppointmentTimeRequestDateOnly)
  }

  @Test
  fun `update initial appointment time should throw error when Appointment time is null and Appointment type is SPECIFIC_DATE_TIME`() {
    val anAppointmentTimeRequestDateNull = anAppointmentTimeRequestDateOnly.copy(appointmentTime = null)
    whenever(
      appointmentService.updateAppointmentTime(
        4,
        anAppointmentTimeRequestDateNull,
      ),
    ).thenThrow(ValidationException("Appointment time must not be null if Appointment Type is SPECIFIC_DATE_TIME"))

    val result = mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentTimeRequestDateNull)),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
      .andReturn()

    assertThat(result.response.contentAsString).contains("Appointment time must not be null if Appointment Type is SPECIFIC_DATE_TIME")

    verify(appointmentService, times(1)).updateAppointmentTime(
      4,
      anAppointmentTimeRequestDateNull,
    )
  }

  @Test
  fun `update initial appointment time not should throw error when Appointment time is null and Appointment type is not SPECIFIC_DATE_TIME`() {
    val anAppointmentTimeRequestDateNull =
      anAppointmentTimeRequestDateOnly.copy(appointmentTime = null, AppointmentTimeType.IMMEDIATE_UPON_RELEASE)

    mvc.perform(
      put("/licence/id/4/appointmentTime")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentTimeRequestDateNull)),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateAppointmentTime(4, anAppointmentTimeRequestDateNull)
  }

  @Test
  fun `update officer contact number`() {
    mvc.perform(
      put("/licence/id/4/contact-number")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(aContactNumberRequest)),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateContactNumber(4, aContactNumberRequest)
  }

  @Test
  fun `update officer contact number - invalid request body`() {
    mvc.perform(
      put("/licence/id/4/contact-number")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes({ })),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
  }

  @Test
  fun `update appointment address`() {
    mvc.perform(
      put("/licence/id/4/appointment-address")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes(anAppointmentAddressRequest)),
    )
      .andExpect(status().isOk)

    verify(appointmentService, times(1)).updateAppointmentAddress(4, anAppointmentAddressRequest)
  }

  @Test
  fun `update appointment address - invalid request body`() {
    mvc.perform(
      put("/licence/id/4/appointment-address")
        .accept(APPLICATION_JSON)
        .contentType(APPLICATION_JSON)
        .content(mapper.writeValueAsBytes({ })),
    )
      .andExpect(status().isBadRequest)
      .andExpect(content().contentType(APPLICATION_JSON))
  }

  private companion object {
    val anUpdateAppointmentPersonRequest = AppointmentPersonRequest(
      appointmentPersonType = AppointmentPersonType.DUTY_OFFICER,
      appointmentPerson = "John Smith",
    )

    val anAppointmentTimeRequest = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(1L).truncatedTo(ChronoUnit.MINUTES),
      appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
    )

    val anAppointmentTimeRequestDateOnly = AppointmentTimeRequest(
      appointmentTime = LocalDateTime.now().plusDays(1L).truncatedTo(ChronoUnit.DAYS),
      appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
    )

    val aContactNumberRequest = ContactNumberRequest(
      telephone = "0114 2566555",
    )

    val anAppointmentAddressRequest = AppointmentAddressRequest(
      appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE",
    )
  }
}
