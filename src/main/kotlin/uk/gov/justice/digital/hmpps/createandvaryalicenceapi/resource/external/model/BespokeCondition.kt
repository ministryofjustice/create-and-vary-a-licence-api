package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.external.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a bespoke condition on a licence")
data class BespokeCondition(
  @Schema(description = "The internal ID for this condition on this licence", example = "98989")
  val id: Long = -1,

  @Schema(description = "The text of this bespoke condition", example = "You should not visit any music venues")
  val text: String? = null,
)
