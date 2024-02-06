package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonView
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition")
data class AdditionalCondition(
  @Schema(description = "The internal ID for this additional condition for this licence", example = "98989")
  val id: Long? = -1,

  @Schema(description = "Coded value for the additional condition", required = true, example = "meetingAddress")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val code: String? = null,

  @Schema(description = "Version number for condition", example = "2.1")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val version: String? = null,

  @Schema(description = "The category of the additional condition", example = "Freedom of movement")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val category: String? = null,

  @Schema(description = "Sequence of this additional condition within the additional conditions", example = "1")
  val sequence: Int? = null,

  @Schema(
    description = "The textual value for this additional condition",
    example = "You must not enter the location [DESCRIPTION]",
  )
  val text: String? = null,

  @Schema(
    description = "The condition text with the users data inserted into the template",
    example = "You must not enter the location Tesco Superstore",
  )
  @get:JsonView(Views.SubjectAccessRequest::class)
  val expandedText: String? = null,

  @Schema(description = "The list of data items entered for this additional condition")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val data: List<AdditionalConditionData> = emptyList(),

  @Schema(description = "The list of file upload summary for this additional condition")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val uploadSummary: List<AdditionalConditionUploadSummary> = emptyList(),

  @Schema(description = "Whether the condition is ready to submit for approval")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val readyToSubmit: Boolean?,
)
