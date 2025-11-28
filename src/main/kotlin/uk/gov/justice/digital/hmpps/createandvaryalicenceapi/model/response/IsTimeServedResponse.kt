package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response indicating whether the parent or root licence is time served")
data class IsTimeServedResponse(
  @field:Schema(description = "Whether the parent or root licence is time served", example = "true")
  val timeServed: Boolean,
)
