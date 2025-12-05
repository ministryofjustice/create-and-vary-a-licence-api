package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema

@JsonTypeName(ConditionTypes.SINGLE_UPLOAD)
@Schema(
  description = "Describes an additional condition on the licence that has a single associated upload",
)
data class SingleUploadAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456") override val id: Long,

  @field:Schema(
    description = "The category to which the additional condition belongs",
    example = "Freedom of movement",
  ) override val category: String,

  @field:Schema(
    description = "Discriminator for condition type",
    example = ConditionTypes.SINGLE_UPLOAD,
  ) override val type: String = ConditionTypes.SINGLE_UPLOAD,

  @field:Schema(
    description = "The code of the condition",
    example = "99195049-f355-46fb-b7d8-aef87a1b19c5",
  ) override val code: String,

  @field:Schema(
    description = "The inputted text for the condition",
    example = "Not to enter the area as defined by the attached map, during the period that X takes place, including all occasions that the event takes place, without the prior permission of your supervising officer.",
  ) override val text: String,

  @field:Schema(
    description = "Whether any image was uploaded when the condition was created",
    example = "true",
  ) val hasImageUpload: Boolean,

) : AdditionalCondition
