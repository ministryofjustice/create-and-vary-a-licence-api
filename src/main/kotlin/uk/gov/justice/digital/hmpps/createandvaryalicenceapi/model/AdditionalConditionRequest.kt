package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition to create/update")
data class AdditionalConditionRequest(
  @Schema(description = "Coded value for the additional condition", example = "meetingAddress")
  val code: String,

  @Schema(description = "The category of the additional condition", example = "Freedom of movement")
  val category: String,

  @Schema(description = "Sequence of this additional condition within the additional conditions", example = "1")
  val sequence: Int? = null,

  @Schema(
    description = "The textual value for this additional condition",
    example = "You must not enter the location [DESCRIPTION]",
  )
  val text: String,
)
