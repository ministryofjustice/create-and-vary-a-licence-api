package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response to an update policy request")
data class PolicyUpdateResponse(
  @field:Schema(description = "Were the standard conditions on the licence updated", example = "true")
  val policyUpdated: Boolean,

  @field:Schema(
    description = "The current policy version of the licence",
    example = "3.0",
  )
  val policyVersion: String,
)
