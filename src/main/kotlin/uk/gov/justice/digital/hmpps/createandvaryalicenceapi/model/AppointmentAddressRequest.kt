package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for updating the address of the initial appointment")
data class AppointmentAddressRequest(
  @field:Schema(
    description = "The address of initial appointment",
    example = "Manchester Probation Service, Unit 4, Smith Street, Stockport, SP1 3DN",
  )
  @field:NotBlank
  val appointmentAddress: String?,
)
