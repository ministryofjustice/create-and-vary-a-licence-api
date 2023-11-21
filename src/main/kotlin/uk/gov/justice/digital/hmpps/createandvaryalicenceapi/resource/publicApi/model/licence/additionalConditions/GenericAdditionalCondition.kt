package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeName(ConditionTypes.STANDARD)
@Schema(
  description = "Describes an instance of a additional condition on the licence",
)
data class GenericAdditionalCondition(
  @Schema(description = "The ID of the condition", example = "123456")
  override val id: Long,

  @get:Schema(
    description = "The category to which the additional condition belongs",
    example = "Residence at a specific place",
  )
  override val category: String,

  @get:Schema(description = "Discriminator for condition type", example = ConditionTypes.STANDARD)
  override val type: String = ConditionTypes.STANDARD,

  @get:Schema(
    description = "The code shared by all conditions of this type",
    example = "5a105297-dce1-4d18-b9ea-4195b46b7594",
  )
  override val code: String,

  @get:Schema(
    description = "A combination of inputted text and template text including any user input",
    example = "You must not enter the location X",
  )
  override val text: String,

) : AdditionalCondition
