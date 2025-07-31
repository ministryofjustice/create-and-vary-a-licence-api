package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each standard condition")
data class StandardCondition(

  @field:Schema(
    description = "The code shared by all instances of this condition",
    example = "5a105297-dce1-4d18-b9ea-4195b46b7594",
  )
  val code: String,

  @field:Schema(description = "The standard condition", example = "Not commit any offence.")
  val text: String,
)
