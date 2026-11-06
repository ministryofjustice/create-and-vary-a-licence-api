package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import java.time.LocalDate

@JsonTypeName(ConditionTypes.ELECTRONIC_MONITORING)
@Schema(
  name = "ElectronicMonitoringAdditionalCondition$latestApiVersionPostFix",
  description = "An electronic monitoring condition with its end date, $latestApiVersion",
)
open class ElectronicMonitoringAdditionalCondition(
  @field:Schema(description = "The ID of the condition", example = "123456")
  override val id: Long,

  @field:Schema(description = "The category to which the condition belongs", example = "Electronic monitoring")
  override val category: String,

  @field:Schema(description = "The code shared by all conditions of this type")
  override val code: String,

  @field:Schema(description = "The inputted text for the instance")
  override val text: String,

  @field:Schema(
    description = "The electronic monitoring end date, or null if no date was recorded, including on historical licences",
    example = "2028-01-16",
  )
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  open val endDate: LocalDate? = null,
) : AdditionalCondition {
  override val type: String = ConditionTypes.ELECTRONIC_MONITORING
}
