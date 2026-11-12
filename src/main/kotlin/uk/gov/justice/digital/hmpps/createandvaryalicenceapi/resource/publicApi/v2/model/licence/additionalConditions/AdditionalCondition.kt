package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix

object ConditionTypes {
  const val CURFEW = "CURFEW"
  const val ELECTRONIC_MONITORING = "ELECTRONIC_MONITORING"
  const val ELECTRONIC_MONITORING_WITH_RESTRICTIONS = "ELECTRONIC_MONITORING_WITH_RESTRICTIONS"
  const val MULTIPLE_EXCLUSION_ZONE = "MULTIPLE_EXCLUSION_ZONE"
  const val MULTIPLE_UPLOADS = "MULTIPLE_UPLOADS"
  const val SINGLE_UPLOAD = "SINGLE_UPLOAD"
  const val STANDARD = "STANDARD"
}

@Schema(
  name = "AdditionalCondition$latestApiVersionPostFix",
  description = "Describes each additional condition on the licence, $latestApiVersion",
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(value = ConditionTypes.CURFEW, schema = CurfewAdditionalCondition::class),
    DiscriminatorMapping(value = ConditionTypes.STANDARD, schema = GenericAdditionalCondition::class),
    DiscriminatorMapping(
      value = ConditionTypes.ELECTRONIC_MONITORING,
      schema = ElectronicMonitoringAdditionalCondition::class,
    ),
    DiscriminatorMapping(
      value = ConditionTypes.ELECTRONIC_MONITORING_WITH_RESTRICTIONS,
      schema = ElectronicMonitoringAdditionalConditionWithRestriction::class,
    ),
    DiscriminatorMapping(
      value = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
      schema = MultipleExclusionZoneAdditionalCondition::class,
    ),
    DiscriminatorMapping(value = ConditionTypes.MULTIPLE_UPLOADS, schema = MultipleUploadAdditionalCondition::class),
    DiscriminatorMapping(value = ConditionTypes.SINGLE_UPLOAD, schema = SingleUploadAdditionalCondition::class),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(
  JsonSubTypes.Type(value = CurfewAdditionalCondition::class, name = ConditionTypes.CURFEW),
  JsonSubTypes.Type(value = ElectronicMonitoringAdditionalCondition::class, name = ConditionTypes.ELECTRONIC_MONITORING),
  JsonSubTypes.Type(
    value = ElectronicMonitoringAdditionalConditionWithRestriction::class,
    name = ConditionTypes.ELECTRONIC_MONITORING_WITH_RESTRICTIONS,
  ),
)
sealed interface AdditionalCondition {
  @get:Schema(description = "The category to which the additional condition belongs")
  val category: String

  @get:Schema(description = "Discriminator for condition type", example = ConditionTypes.STANDARD)
  val type: String

  @get:Schema(description = "The code shared by all conditions of this type")
  val code: String

  @get:Schema(description = "The ID of this condition instance", example = "123456")
  val id: Long

  @get:Schema(description = "The inputted text for the instance")
  val text: String
}
