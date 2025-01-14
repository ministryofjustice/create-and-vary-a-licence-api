package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition")
data class SarAdditionalCondition(

  @Schema(description = "Coded value for the additional condition", required = true, example = "meetingAddress")
  val code: String? = null,

  @Schema(description = "Version number for condition", example = "2.1")
  val version: String? = null,

  @Schema(description = "The category of the additional condition", example = "Freedom of movement")
  val category: String? = null,

  @Schema(
    description = "The condition text with the users data inserted into the template",
    example = "You must not enter the location Tesco Superstore",
  )
  val expandedText: String? = null,

  @Schema(description = "The list of file upload summary for this additional condition")
  val uploadSummary: List<SarAdditionalConditionUploadSummary> = emptyList(),
)
