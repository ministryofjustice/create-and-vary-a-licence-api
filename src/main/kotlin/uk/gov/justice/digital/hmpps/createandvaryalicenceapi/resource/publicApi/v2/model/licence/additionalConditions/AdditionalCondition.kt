package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ConditionTypes.CURFEW
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ConditionTypes.ELECTRONIC_MONITORING
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ConditionTypes.ELECTRONIC_MONITORING_WITH_RESTRICTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ConditionTypes.IMAGE_UPLOAD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.ConditionTypes.STANDARD

object ConditionTypes {
  const val CURFEW = "CURFEW"
  const val ELECTRONIC_MONITORING = "ELECTRONIC_MONITORING"
  const val ELECTRONIC_MONITORING_WITH_RESTRICTIONS = "ELECTRONIC_MONITORING_WITH_RESTRICTIONS"
  const val IMAGE_UPLOAD = "IMAGE_UPLOAD"
  const val STANDARD = "STANDARD"
}

@Schema(
  name = "AdditionalCondition$latestApiVersionPostFix",
  description = "Describes each additional condition on the licence, $latestApiVersion",
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(value = CURFEW, schema = CurfewAdditionalCondition::class),
    DiscriminatorMapping(value = STANDARD, schema = GenericAdditionalCondition::class),
    DiscriminatorMapping(value = ELECTRONIC_MONITORING, schema = ElectronicMonitoringAdditionalCondition::class),
    DiscriminatorMapping(
      value = ELECTRONIC_MONITORING_WITH_RESTRICTIONS,
      schema = ElectronicMonitoringAdditionalConditionWithRestrictions::class,
    ),
    DiscriminatorMapping(value = IMAGE_UPLOAD, schema = ImageUploadAdditionalCondition::class),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(
  JsonSubTypes.Type(value = CurfewAdditionalCondition::class, name = CURFEW),
  JsonSubTypes.Type(
    value = ElectronicMonitoringAdditionalCondition::class,
    name = ELECTRONIC_MONITORING,
  ),
  JsonSubTypes.Type(
    value = ElectronicMonitoringAdditionalConditionWithRestrictions::class,
    name = ELECTRONIC_MONITORING_WITH_RESTRICTIONS,
  ),
)
sealed interface AdditionalCondition {
  @get:Schema(description = "The category to which the additional condition belongs")
  val category: String

  @get:Schema(description = "Discriminator for condition type", example = STANDARD)
  val type: String

  @get:Schema(description = "The code shared by all conditions of this type")
  val code: String

  @get:Schema(description = "The ID of this condition instance", example = "123456")
  val id: Long

  @get:Schema(description = "The inputted text for the instance")
  val text: String
}
