package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonTypeName(ConditionTypes.ELECTRONIC_MONITORING)
@Schema(
  description = "Describes an instance of the electronic monitoring condition on the licence",
)
open class ElectronicMonitoringAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456") override val id: Long,

  @field:Schema(
    description = "The category to which the additional condition belongs",
    example = "Electronic monitoring",
  ) override val category: String,

  @field:Schema(
    description = "Discriminator for condition type",
    example = ConditionTypes.ELECTRONIC_MONITORING,
  ) override val type: String = ConditionTypes.ELECTRONIC_MONITORING,

  @field:Schema(
    description = "The code shared by all conditions of this type",
    example = "fd129172-bdd3-4d97-a4a0-efd7b47a49d4",
  ) override val code: String,

  @field:Schema(
    description = "The inputted text for the instance",
    example = "Allow person(s) as designated by your supervising officer to install an electronic monitoring tag on you and access to install any associated equipment in your property, and for the purpose of ensuring that equipment is functioning correctly. You must not damage or tamper with these devices and ensure that the tag is charged, and report to your supervising officer and the EM provider immediately if the tag or the associated equipment are not working correctly.",
  ) override val text: String,

  @field:Schema(
    description = "The electronic monitoring end date. May be null or omitted when not entered, including licences using policies before v4 which did not collect this date.",
    example = "2028-01-16",
    type = "string",
    format = "date",
    nullable = true,
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  @field:JsonInclude(JsonInclude.Include.NON_NULL)
  open val endDate: LocalDate? = null,

) : AdditionalCondition
