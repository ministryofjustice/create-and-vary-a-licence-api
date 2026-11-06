package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix

@JsonTypeName(ConditionTypes.SINGLE_UPLOAD)
@Schema(
  name = "SingleUploadAdditionalCondition$latestApiVersionPostFix",
  description = "Describes a condition with a single associated upload, $latestApiVersion",
)
data class SingleUploadAdditionalCondition(
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
  override val type: String = ConditionTypes.SINGLE_UPLOAD
}
