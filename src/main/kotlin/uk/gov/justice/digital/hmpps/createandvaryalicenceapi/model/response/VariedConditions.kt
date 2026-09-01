package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ImageUploadSummary

@Schema(description = "Describes a type of condition that can be varied on a licence.")
object ConditionType {
  const val AP = "AP"
  const val BESPOKE = "BESPOKE"
}

@Schema(
  description = "Describes a condition that has changed when a licence was varied.",
  oneOf = [VariedAdditionalCondition::class, VariedBespokeCondition::class],
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(value = ConditionType.AP, schema = VariedAdditionalCondition::class),
    DiscriminatorMapping(value = ConditionType.BESPOKE, schema = VariedBespokeCondition::class),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
sealed interface Condition {
  val type: String
  val category: String?
  val condition: String
}

@Schema(description = "Describes an additional condition that has changed when a licence was varied.")
@JsonTypeName(ConditionType.AP)
data class VariedAdditionalCondition(
  override val type: String = ConditionType.AP,
  override val category: String?,
  override val condition: String,
  val uploadSummaries: List<ImageUploadSummary>? = emptyList(),
) : Condition

@Schema(description = "Describes a bespoke condition that has changed when a licence was varied.")
@JsonTypeName(ConditionType.BESPOKE)
data class VariedBespokeCondition(
  override val type: String = ConditionType.BESPOKE,
  override val category: String,
  override val condition: String,
) : Condition

data class VariedConditions(
  val licenceConditionsAdded: List<Condition>,
  val licenceConditionsRemoved: List<Condition>,
  val licenceConditionsAmended: List<Condition>,
)
