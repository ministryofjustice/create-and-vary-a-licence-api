package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for editing a variation licence")
data class EditVariationRequest(
  @field:Schema(description = "The username of the person who is updating this status", example = "X12333")
  @field:NotBlank
  val username: String,
)
