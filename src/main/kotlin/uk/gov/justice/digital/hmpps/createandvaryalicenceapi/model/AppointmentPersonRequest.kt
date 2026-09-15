package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertFalse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentType.NO_APPOINTMENT_NEEDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentType.SPECIFIC_PERSON

@Schema(description = "Request object for updating the person the person on probation will meet at the initial appointment")
data class AppointmentPersonRequest(

  @field:Schema(description = "The type of appointment with for the initial appointment", example = "SPECIFIC_PERSON")
  val appointmentPersonType: AppointmentType = SPECIFIC_PERSON,

  @field:Schema(
    description = "The name of the person the person on probation will meet at the initial appointment",
    example = "John Smith",
  )
  val appointmentPerson: String?,
) {
  @JsonIgnore
  @AssertFalse(message = "Appointment person must be absent when an appointment is not needed.")
  fun isPersonIncorrectlySpecified() = appointmentPersonType == NO_APPOINTMENT_NEEDED && appointmentPerson != null

  @JsonIgnore
  @AssertFalse(message = "Appointment person must not be missing if Appointment With Type is SPECIFIC_PERSON")
  fun isPersonIncorrectlyUnspecified() = appointmentPersonType == SPECIFIC_PERSON && appointmentPerson.isNullOrBlank()
}
