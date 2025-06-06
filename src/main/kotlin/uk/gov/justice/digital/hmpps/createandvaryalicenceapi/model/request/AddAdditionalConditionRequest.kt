package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition request")
data class AddAdditionalConditionRequest(
  @Schema(description = "Coded value for the additional condition", example = "meetingAddress")
  val conditionCode: String,

  @Schema(description = "Condition type, either AP or PSS", example = "AP")
  val conditionType: String,

  @Schema(description = "The category of the additional condition", example = "Freedom of movement")
  val conditionCategory: String,

  @Schema(description = "Sequence of this additional condition within the additional conditions", example = "1")
  val sequence: Int? = null,

  @Schema(
    description = "The textual value for this additional condition",
    example = "You must not enter the location [DESCRIPTION]",
  )
  val conditionText: String,

  @Schema(
    description = "The condition text with the users data inserted into the template",
    example = "You must not enter the location Tesco Superstore",
  )
  val expandedText: String,
)
