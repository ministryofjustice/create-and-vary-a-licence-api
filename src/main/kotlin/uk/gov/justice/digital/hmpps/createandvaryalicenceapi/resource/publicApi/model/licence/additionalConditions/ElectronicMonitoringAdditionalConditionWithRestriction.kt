package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType
import java.time.LocalDate

@Schema(
  description = "Describes an electronic monitoring condition with restrictions",
)
data class ElectronicMonitoringAdditionalConditionWithRestriction(
  override val id: Long,
  override val category: String,
  override val type: String = ConditionTypes.ELECTRONIC_MONITORING_WITH_RESTRICTIONS,
  override val code: String,
  override val text: String,

  @field:Schema(
    description = "The type of electronic monitoring that is included by this condition",
    example = "['ALCOHOL']",
    implementation = ElectronicMonitoringType::class,
  ) val restrictions: List<ElectronicMonitoringType>,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  @field:JsonInclude(JsonInclude.Include.NON_NULL)
  override val endDate: LocalDate? = null,
) : ElectronicMonitoringAdditionalCondition(
  id = id,
  category = category,
  type = type,
  code = code,
  text = text,
  endDate = endDate,
)
