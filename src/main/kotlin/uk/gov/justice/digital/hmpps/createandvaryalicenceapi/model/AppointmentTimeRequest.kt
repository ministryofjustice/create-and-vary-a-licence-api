package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

@Schema(description = "Request object for updating the date and time of the initial appointment")
data class AppointmentTimeRequest(
  @Schema(description = "The date and time of the initial appointment", example = "12/12/2021 10:35")
  @NotNull
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val appointmentTime: LocalDateTime,
)
