package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.external.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the data entered for an additional condition")
data class AdditionalConditionData(
  @Schema(description = "The internal ID of this data item, for this condition on this licence", example = "98989")
  val id: Long = -1,

  @Schema(description = "The field name of this data item for this condition on this licence", example = "location")
  val field: String? = null,

  @Schema(description = "The value of this data item", example = "Norfolk")
  val value: String? = null,
  )
