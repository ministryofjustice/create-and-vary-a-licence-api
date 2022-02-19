package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank

@Schema(description = "Request object for updating the VLO discussion")
data class UpdateVloDiscussionRequest(
  @Schema(description = "Whether or not the licence variation has been discussed with a VLO", example = "Yes")
  @field:NotBlank
  val vloDiscussion: String,
)
