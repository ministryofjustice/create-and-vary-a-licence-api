package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix

@JsonTypeName(ConditionTypes.STANDARD)
@Schema(
  name = "GenericAdditionalCondition$latestApiVersionPostFix",
  description = "Describes an instance of an additional condition on the licence, $latestApiVersion",
)
data class GenericAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456")
  override val id: Long,

  @field:Schema(description = "The category to which the condition belongs")
  override val category: String,

  @field:Schema(description = "The code shared by all conditions of this type")
  override val code: String,

  @field:Schema(description = "A combination of template text and user input")
  override val text: String,
) : AdditionalCondition {
  override val type: String = ConditionTypes.STANDARD
}
