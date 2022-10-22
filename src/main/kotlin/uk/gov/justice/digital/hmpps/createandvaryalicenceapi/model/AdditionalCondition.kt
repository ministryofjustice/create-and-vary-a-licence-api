package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition")
data class AdditionalCondition(
  @Schema(description = "The internal ID for this additional condition for this licence", example = "98989")
  val id: Long? = -1,

  @Schema(description = "Coded value for the additional condition", example = "meetingAddress")
  val code: String? = null,

  @Schema(description = "Version number for condition", example = "2.1")
  val version: String? = null,

  @Schema(description = "The category of the additional condition", example = "Freedom of movement")
  val category: String? = null,

  @Schema(description = "Sequence of this additional condition within the additional conditions", example = "1")
  val sequence: Int? = null,

  @Schema(description = "The textual value for this additional condition", example = "You must not enter the location [DESCRIPTION]")
  val text: String? = null,

  @Schema(description = "The condition text with the users data inserted into the template", example = "You must not enter the location Tesco Superstore")
  val expandedText: String? = null,

  @Schema(description = "The list of data items entered for this additional condition")
  val data: List<AdditionalConditionData> = emptyList(),

  @Schema(description = "The list of file upload summary for this additional condition")
  val uploadSummary: List<AdditionalConditionUploadSummary> = emptyList(),
)
