package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an additional condition")
data class SarAdditionalCondition(

  @field:Schema(description = "Coded value for the additional condition", required = true, example = "meetingAddress")
  val code: String? = null,

  @field:Schema(description = "Version number for condition", example = "2.1")
  val version: String? = null,

  @field:Schema(description = "The category of the additional condition", example = "Freedom of movement")
  val category: String? = null,

  @field:Schema(
    description = "The condition text with the users data inserted into the template",
    example = "You must not enter the location Tesco Superstore",
  )
  val text: String? = null,

  @field:Schema(description = "The list of file upload summary for this additional condition")
  val uploadSummary: List<SarAdditionalConditionUploadSummary> = emptyList(),
)
