package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeName(ConditionTypes.MULTIPLE_EXCLUSION_ZONE)
@Schema(
  description = "Describes an instance of the multiple exclusion zone condition on the licence",
)
data class ExclusionZoneAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456") override val id: Long,

  @field:Schema(
    description = "The category to which the additional condition belongs",
    example = "Freedom of movement",
  ) override val category: String,

  @field:Schema(
    description = "Discriminator for condition type",
    example = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
  ) override val type: String = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,

  @field:Schema(
    description = "The code shared by all conditions of this type",
    example = "0f9a20f4-35c7-4c77-8af8-f200f153fa11",
  ) override val code: String,

  @field:Schema(
    description = "The inputted text for the instance",
    example = "Not to enter the area of X, as defined by the attached map, without the prior approval of your supervising officer.",
  ) override val text: String,

  @field:Schema(
    description = "Whether any image was uploaded when the instance was created",
    example = "true",
  ) val hasImageUpload: Boolean,

) : AdditionalCondition
