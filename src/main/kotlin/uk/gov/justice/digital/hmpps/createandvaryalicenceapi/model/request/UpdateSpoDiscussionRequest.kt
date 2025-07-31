package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for updating the SPO discussion")
data class UpdateSpoDiscussionRequest(
  @field:Schema(description = "Whether or not the licence variation has been discussed with an SPO", example = "Yes")
  @field:NotBlank
  val spoDiscussion: String,
)
