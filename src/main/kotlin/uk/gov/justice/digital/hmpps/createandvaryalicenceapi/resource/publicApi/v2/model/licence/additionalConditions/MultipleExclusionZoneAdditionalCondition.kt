package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix

@JsonTypeName(ConditionTypes.MULTIPLE_EXCLUSION_ZONE)
@Schema(
  name = "MultipleExclusionZoneAdditionalCondition$latestApiVersionPostFix",
  description = "Describes an instance of the multiple exclusion zone condition on the licence, $latestApiVersion",
)
data class MultipleExclusionZoneAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456")
  override val id: Long,

  @field:Schema(description = "The category to which the condition belongs")
  override val category: String,

  @field:Schema(description = "The code shared by all conditions of this type")
  override val code: String,

  @field:Schema(description = "The inputted text for the instance")
  override val text: String,

  @field:Schema(description = "The description of the uploaded file, if provided")
  val description: String?,

  @field:Schema(description = "Whether any image was uploaded")
  val hasImageUpload: Boolean,
) : AdditionalCondition {
  override val type: String = ConditionTypes.MULTIPLE_EXCLUSION_ZONE
}
