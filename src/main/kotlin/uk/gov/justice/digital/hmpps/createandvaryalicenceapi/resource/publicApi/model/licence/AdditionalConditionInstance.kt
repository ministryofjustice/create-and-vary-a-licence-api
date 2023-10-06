package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Constants.ELECTRONIC_MONITORING
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Constants.STANDARD

object Constants {
  const val ELECTRONIC_MONITORING = "ELECTRONIC_MONITORING"
  const val STANDARD = "STANDARD"
}

@Schema(
  description = "Describes each instance of the same additional condition on the licence, each one of these will be an AdditionalConditionInstance or AdditionalConditionElectronicMonitoringInstance",
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(value = STANDARD, schema = AdditionalConditionInstanceStandard::class),
    DiscriminatorMapping(
      value = ELECTRONIC_MONITORING,
      schema = AdditionalConditionElectronicMonitoringInstance::class,
    ),
  ],
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface AdditionalConditionInstance {
  @get:Schema(description = "Discriminator for instance type", example = STANDARD)
  val type: String

  @get:Schema(description = "The ID of the instance", example = "123456")
  val id: Long

  @get:Schema(description = "The inputted text for the instance", example = "You must not enter the location X")
  val text: String

  @get:Schema(description = "Whether any image was uploaded when the instance was created", example = "true")
  val hasImageUpload: Boolean
}

@JsonTypeName("STANDARD")
@Schema(
  description = "Describes each instance of the same additional condition on the licence",
  allOf = [AdditionalConditionInstance::class],
)
data class AdditionalConditionInstanceStandard(
  @get:Schema(description = "Discriminator for instance type", example = STANDARD)
  override val type: String = STANDARD,

  override val id: Long,

  override val text: String,

  override val hasImageUpload: Boolean,
) : AdditionalConditionInstance

@JsonTypeName(ELECTRONIC_MONITORING)
@Schema(
  description = "Describes an instance of the electronic monitoring condition on the licence",
  allOf = [AdditionalConditionInstance::class],
)
data class AdditionalConditionElectronicMonitoringInstance(
  @get:Schema(description = "Discriminator for instance type", example = ELECTRONIC_MONITORING)
  override val type: String = ELECTRONIC_MONITORING,

  override val id: Long,

  override val text: String,

  override val hasImageUpload: Boolean = false,

  @Schema(description = "The type of electronic monitoring that is included by this condition", example = "['ALCOHOL']")
  val electronicMonitoringTypes: List<ElectronicMonitoringType>,
) : AdditionalConditionInstance

enum class ElectronicMonitoringType {
  EXCLUSION_ZONE,
  CURFEW,
  LOCATION_MONITORING,
  ATTENDANCE_AT_APPOINTMENTS,
  ALCOHOL_MONITORING,
  ALCOHOL_ABSTINENCE,
}
