package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class StandardConditions(
  @field:JsonProperty("AP")
  val standardConditionsAp: List<StandardConditionAp>,
  // PSS has been repealed - these are retained only to correctly render/vary existing historical PSS licences
  @field:JsonProperty("PSS")
  val standardConditionsPss: List<StandardConditionPss>,
)
