package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime

@Schema(description = "Request object for updating the date and time of the initial appointment")
data class AppointmentTimeRequest(
  @Schema(description = "The date and time of the initial appointment", example = "12/12/2021 10:35")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val appointmentTime: LocalDateTime,
  @Schema(description = "The type of appointment time at the initial appointment", example = "IMMEDIATE_UPON_RELEASE")
  val appointmentTimeType: AppointmentTimeType,
)
