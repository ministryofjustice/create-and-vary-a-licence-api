package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema

object ConditionTypes {
  const val ELECTRONIC_MONITORING = "ELECTRONIC_MONITORING"
  const val MULTIPLE_EXCLUSION_ZONE = "MULTIPLE_EXCLUSION_ZONE"
  const val STANDARD = "STANDARD"
}

@Schema(
  description = "Describes each additional condition on the licence, A discriminator exists to allow specific types of conditions to contain additional info",
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(value = ConditionTypes.STANDARD, schema = StandardAdditionalCondition::class),
    DiscriminatorMapping(
      value = ConditionTypes.ELECTRONIC_MONITORING,
      schema = ElectronicMonitoringAdditionalCondition::class,
    ),
    DiscriminatorMapping(
      value = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
      schema = MultipleExclusionZoneAdditionalCondition::class,
    ),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface AdditionalCondition {
  @get:Schema(
    description = "The category to which the additional condition belongs",
    example = "Residence at a specific place",
  )
  val category: String

  @get:Schema(description = "Discriminator for condition type", example = ConditionTypes.STANDARD)
  val type: String

  @get:Schema(
    description = "The code shared by all conditions of this type",
    example = "5a105297-dce1-4d18-b9ea-4195b46b7594",
  )
  val code: String

  @get:Schema(description = "The ID of this condition instance", example = "123456")
  val id: Long

  @get:Schema(description = "The inputted text for the instance", example = "You must not enter the location X")
  val text: String
}
