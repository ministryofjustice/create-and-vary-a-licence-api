package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for updating the contact number of the officer on a licence")
data class ContactNumberRequest(
  @field:Schema(
    description = "The UK telephone number to contact the person the offender should meet for their initial meeting",
    example = "0114 2557665",
  )
  @field:NotBlank
  val telephone: String?,
)
