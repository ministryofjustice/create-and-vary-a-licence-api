package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition")
data class AdditionalCondition(
  @field:Schema(description = "The internal ID for this additional condition for this licence", example = "98989")
  val id: Long? = -1,

  @field:Schema(description = "Coded value for the additional condition", required = true, example = "meetingAddress")
  val code: String? = null,

  @field:Schema(description = "Version number for condition", example = "2.1")
  val version: String? = null,

  @field:Schema(description = "The category of the additional condition", example = "Freedom of movement")
  val category: String? = null,

  @field:Schema(description = "Sequence of this additional condition within the additional conditions", example = "1")
  val sequence: Int? = null,

  @field:Schema(
    description = "The textual value for this additional condition",
    example = "You must not enter the location [DESCRIPTION]",
  )
  val text: String? = null,

  @field:Schema(
    description = "The condition text with the users data inserted into the template",
    example = "You must not enter the location Tesco Superstore",
  )
  val expandedText: String? = null,

  @field:Schema(description = "The list of data items entered for this additional condition")
  val data: List<AdditionalConditionData> = emptyList(),

  @field:Schema(description = "The list of file upload summary for this additional condition")
  val uploadSummary: List<AdditionalConditionUploadSummary> = emptyList(),

  @field:Schema(description = "Whether the condition is ready to submit for approval")
  val readyToSubmit: Boolean?,

  @field:Schema(description = "Whether the condition requires input from the user")
  val requiresInput: Boolean?,
)
