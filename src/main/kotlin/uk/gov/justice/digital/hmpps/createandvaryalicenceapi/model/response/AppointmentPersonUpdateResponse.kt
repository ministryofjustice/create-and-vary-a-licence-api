package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response to an update appointment person request")
data class AppointmentPersonUpdateResponse(
  @field:Schema(
    description = "Whether the licence still needs an appointment time to be set, given the new appointment person",
    example = "true",
  )
  val missingAppointmentTime: Boolean,
)
