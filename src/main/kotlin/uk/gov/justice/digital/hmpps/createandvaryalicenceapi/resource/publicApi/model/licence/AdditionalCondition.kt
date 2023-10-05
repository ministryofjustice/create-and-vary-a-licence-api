package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each additional condition on the licence")
data class AdditionalCondition(

  @Schema(description = "The additional condition's unique code", example = "5a105297-dce1-4d18-b9ea-4195b46b7594")
  val code: String,

  @Schema(
    description = "The category to which the additional condition belongs",
    example = "Residence at a specific place",
  )
  val category: String,

  @ArraySchema(
    arraySchema = Schema(discriminatorProperty = "type"),
    schema = Schema(
      description = "Individual instances of this condition that appear on the licence. Most conditions can only appear on a licence once so would have a single instance but a subset can appear multiple times.",
      implementation = AdditionalConditionInstance::class,
    ),
  )
  val instances: List<AdditionalConditionInstance>,

)
