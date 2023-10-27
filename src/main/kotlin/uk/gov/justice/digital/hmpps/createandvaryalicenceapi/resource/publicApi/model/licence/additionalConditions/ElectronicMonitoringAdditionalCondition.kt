package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema

enum class ElectronicMonitoringType {
  EXCLUSION_ZONE, CURFEW, LOCATION_MONITORING, ATTENDANCE_AT_APPOINTMENTS, ALCOHOL_MONITORING, ALCOHOL_ABSTINENCE,
}

@JsonTypeName(ConditionTypes.ELECTRONIC_MONITORING)
@Schema(
  description = "Describes an instance of the electronic monitoring condition on the licence",
)
data class ElectronicMonitoringAdditionalCondition(
  @Schema(description = "The ID of the condition", example = "123456") override val id: Long,

  @get:Schema(
    description = "The category to which the additional condition belongs",
    example = "Electronic monitoring",
  ) override val category: String,

  @get:Schema(
    description = "Discriminator for condition type",
    example = ConditionTypes.ELECTRONIC_MONITORING,
  ) override val type: String = ConditionTypes.ELECTRONIC_MONITORING,

  @get:Schema(
    description = "The code shared by all conditions of this type",
    example = "fd129172-bdd3-4d97-a4a0-efd7b47a49d4",
  ) override val code: String,

  @get:Schema(
    description = "The inputted text for the instance",
    example = "Allow person(s) as designated by your supervising officer to install an electronic monitoring tag on you and access to install any associated equipment in your property, and for the purpose of ensuring that equipment is functioning correctly. You must not damage or tamper with these devices and ensure that the tag is charged, and report to your supervising officer and the EM provider immediately if the tag or the associated equipment are not working correctly. This will be for the purpose of monitoring your ALCOHOL licence condition(s) unless otherwise authorised by your supervising officer.",
  ) override val text: String,

  @Schema(
    description = "The type of electronic monitoring that is included by this condition",
    example = "['ALCOHOL']",
  ) val electronicMonitoringTypes: List<ElectronicMonitoringType>,

) : AdditionalCondition
