package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank

@Schema(description = "Request object for updating the person the person on probation will meet at the initial appointment")
data class AppointmentPersonRequest(

  @Schema(
    description = "The name of the person the person on probation will meet at the initial appointment",
    example = "John Smith"
  )
  @field:NotBlank
  val appointmentPerson: String?,
)
