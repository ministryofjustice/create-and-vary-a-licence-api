package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a bespoke condition on a licence")
data class BespokeCondition(
  @field:Schema(description = "The internal ID for this condition on this licence", example = "98989")
  val id: Long = -1,

  @field:Schema(description = "The sequence of this bespoke condition on this licence", example = "1")
  val sequence: Int? = null,

  @field:Schema(description = "The text of this bespoke condition", example = "You should not visit any music venues")
  val text: String? = null,
)
