package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class Appointment(

  @Schema(description = "The name of the contact for the appointment", example = "Jane Doe")
  val contact: String,

  @Schema(description = "The date and time of the appointment", example = "03/10/2023 15:49:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val time: LocalDateTime,

  @Schema(
    description = "The address where the appointment will take place",
    example = "Test Probation Service, Test Street, Test, T3 1ST",
  )
  val address: String,

  @Schema(description = "The contact number for the contact", example = "0123 456 7890")
  val phoneNumber: String,
)
