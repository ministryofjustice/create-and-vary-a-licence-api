package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each additional condition on the licence policy")
data class AdditionalConditionData(

  @Schema(description = "The additional condition's unique code", example = "abcd-1234-efgh")
  val code: String,

  @Schema(description = "The additional condition", example = "Test condition")
  val text: String,

  @Schema(description = "The category to which the additional condition belongs", example = "Residence at a specific place")
  val category: String,

  @Schema(description = "The shorthand version of the category to which the additional condition belongs", example = "Residence")
  val categoryShort: String?,

  @Schema(description = "Indicates whether the code requires any user input", example = "true")
  val requiresInput: Boolean,
)
