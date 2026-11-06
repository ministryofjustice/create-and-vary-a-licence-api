package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions

import com.fasterxml.jackson.annotation.JsonTypeName
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ElectronicMonitoringType
import java.time.LocalDate

@JsonTypeName(ConditionTypes.ELECTRONIC_MONITORING_WITH_RESTRICTIONS)
@Schema(
  name = "ElectronicMonitoringAdditionalConditionWithRestriction$latestApiVersionPostFix",
  description = "An electronic monitoring condition with restrictions and an end date, $latestApiVersion",
)
class ElectronicMonitoringAdditionalConditionWithRestriction(
  id: Long,
  category: String,
  code: String,
  text: String,
  override val endDate: LocalDate? = null,

  @field:Schema(
    description = "The types of electronic monitoring included by this condition",
    example = "['ALCOHOL']",
  )
  val restrictions: List<ElectronicMonitoringType>,
) : ElectronicMonitoringAdditionalCondition(
  id = id,
  category = category,
  code = code,
  text = text,
  endDate = endDate,
) {
  override val type: String = ConditionTypes.ELECTRONIC_MONITORING_WITH_RESTRICTIONS
}
