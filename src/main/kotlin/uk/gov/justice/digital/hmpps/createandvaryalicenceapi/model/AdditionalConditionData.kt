package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the data entered for an additional condition")
data class AdditionalConditionData(
  @Schema(description = "The internal ID of this data item, for this condition on this licence", example = "98989")
  val id: Long = -1,

  @Schema(description = "The sequence of this data item, for this condition on this licence", example = "1")
  val sequence: Int = -1,

  @Schema(description = "The description of this data item for this condition on this licence", example = "Location")
  val description: String? = null,

  @Schema(description = "The format of this data item", example = "TEXT")
  val format: String? = null,

  @Schema(description = "The value of this data item", example = "Norfolk")
  val value: String? = null,
)
