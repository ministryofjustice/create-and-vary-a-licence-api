package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType

@Schema(description = "Request object for updating the person the person on probation will meet at the initial appointment")
data class AppointmentPersonRequest(

  @field:Schema(description = "The type of appointment with for the initial appointment", example = "SPECIFIC_PERSON")
  val appointmentPersonType: AppointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,

  @field:Schema(
    description = "The name of the person the person on probation will meet at the initial appointment",
    example = "John Smith",
  )
  val appointmentPerson: String?,
)
