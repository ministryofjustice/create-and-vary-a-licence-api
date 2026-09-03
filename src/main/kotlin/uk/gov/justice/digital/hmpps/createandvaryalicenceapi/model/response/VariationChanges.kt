package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a type of condition that can be varied on a licence.")
object ConditionType {
  const val AP = "AP"
  const val BESPOKE = "BESPOKE"
}

@Schema(description = "Describes a image uploaded linked to a condition.")
data class ImageUploadSummary(
  @get:Schema(description = "The text associated with the image", example = "An exclusion zone map")
  val text: String? = null,

  @get:Schema(
    description = "The description of the image",
    example = "A map showing where licence holder cannot enter.",
  )
  val description: String? = null,

  @get:Schema(
    description = "The thumbnail for the  exclusion zone map as a base64-encoded JPEG image",
    example = "Base64 string",
  )
  val thumbnailImage: String?,
)

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
  @get:Schema(description = "The type of licence condition", example = "AP")
  val type: String

  @get:Schema(
    description = "The category code of the condition",
    example = "Making or maintaining contact with a person",
  )
  val category: String?

  @get:Schema(
    description = "The condition text",
    example = "Receive home visits from a Mental Health Worker.",
  )
  val condition: String
}

@Schema(description = "Describes an additional condition that has changed when a licence was varied.")
@JsonTypeName(ConditionType.AP)
data class VariedAdditionalCondition(
  override val type: String = ConditionType.AP,
  override val category: String?,
  override val condition: String,
  val uploadSummaries: List<ImageUploadSummary> = emptyList(),
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

@Schema(description = "Describes changes between a varied licence and it's parent.")
data class VariationChanges(
  @get:Schema(description = "A list of licence conditions that have been added to the variation")
  val licenceConditionsAdded: List<Condition>,

  @get:Schema(description = "A list of licence conditions that have been removed from the variation")
  val licenceConditionsRemoved: List<Condition>,

  @get:Schema(description = "A list of licence conditions that have been amended in the variation")
  val licenceConditionsAmended: List<Condition>,

  @get:Schema(description = "Has the curfew address been updated in the variation", example = "true")
  val hasUpdatedCurfewAddress: Boolean,

  @get:Schema(description = "Have the curfew hours been updated in the variation", example = "false")
  val hasUpdatedCurfewHours: Boolean,
)
