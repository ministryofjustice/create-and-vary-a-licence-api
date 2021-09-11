package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a standard condition on this licence")
data class StandardCondition(

  @Schema(description = "The internal ID for this standard condition on this licence", example = "98987")
  val id: Long? = null,

  @Schema(description = "The coded value for this standard", example = "generalGoodBehaviour")
  val code: String? = null,

  @Schema(description = "The sequence of this standard condition", example = "1")
  val sequence: Int? = null,

  @Schema(description = "The text of this standard condition", example = "Be of generally good behaviour")
  val text: String? = null,
)
