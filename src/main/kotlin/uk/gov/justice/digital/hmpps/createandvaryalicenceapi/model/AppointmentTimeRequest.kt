package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertFalse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType.SPECIFIC_DATE_TIME
import java.time.LocalDateTime

@Schema(description = "Request object for updating the date and time of the initial appointment")
data class AppointmentTimeRequest(
  @field:Schema(description = "The date and time of the initial appointment", example = "12/12/2021 10:35")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val appointmentTime: LocalDateTime?,
  @field:Schema(
    description = "The type of appointment time at the initial appointment",
    example = "IMMEDIATE_UPON_RELEASE",
  )
  val appointmentTimeType: AppointmentTimeType = SPECIFIC_DATE_TIME,
) {
  @JsonIgnore
  @AssertFalse(message = "Appointment time must not be null if Appointment time type is SPECIFIC_DATE_TIME")
  fun isMissingTime() = appointmentTimeType == SPECIFIC_DATE_TIME && appointmentTime == null
}
