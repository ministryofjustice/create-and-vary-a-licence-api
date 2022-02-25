package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank

@Schema(description = "Request object for referring a licence variation")
data class ReferVariationRequest(
  @Schema(description = "A large string containing rich text markup. A reason for referring the licence variation.")
  @field:NotBlank
  val reasonForReferral: String,
)
