package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for updating the reason for variation")
data class UpdateReasonForVariationRequest(
  @Schema(description = "A large string containing rich text markup. A reason for varying the licence.")
  @field:NotBlank
  val reasonForVariation: String,
)
