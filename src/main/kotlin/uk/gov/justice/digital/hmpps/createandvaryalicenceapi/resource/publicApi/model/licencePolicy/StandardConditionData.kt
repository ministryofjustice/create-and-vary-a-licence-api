package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each standard condition")
data class StandardConditionData(

  @Schema(description = "The standard condition's unique code", example = "abcd-1234-efgh")
  val code: String,

  @Schema(description = "The standard condition", example = "Test condition")
  val text: String,
)
