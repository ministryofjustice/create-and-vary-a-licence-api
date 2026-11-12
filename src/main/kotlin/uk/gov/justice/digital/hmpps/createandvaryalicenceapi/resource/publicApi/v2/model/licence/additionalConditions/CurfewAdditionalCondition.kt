package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import java.time.LocalTime

@JsonTypeName(ConditionTypes.CURFEW)
@Schema(
  name = "CurfewAdditionalCondition$latestApiVersionPostFix",
  description = "A curfew condition with its daily curfew times, $latestApiVersion",
)
data class CurfewAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456")
  override val id: Long,

  @field:Schema(description = "The category to which the condition belongs", example = "Curfew arrangement")
  override val category: String,

  @field:Schema(description = "The code shared by all conditions of this type")
  override val code: String,

  @field:Schema(description = "The inputted text for the instance")
  override val text: String,

  @field:Schema(description = "The daily curfew periods in condition order; empty when no times have been entered")
  val curfewTimes: List<CurfewTime>,
) : AdditionalCondition {
  override val type: String = ConditionTypes.CURFEW
}

@Schema(name = "CurfewTime$latestApiVersionPostFix", description = "A daily curfew period, $latestApiVersion")
data class CurfewTime(
  @field:Schema(description = "The curfew start time in 24-hour HH:mm format", type = "string", example = "19:00")
  @field:JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @field:Schema(description = "The curfew end time in 24-hour HH:mm format", type = "string", example = "07:00")
  @field:JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,
)
