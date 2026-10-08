package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class StandardConditions(
  @field:JsonProperty("AP")
  val standardConditionsAp: List<StandardConditionAp>,

  // PSS has been repealed - retained only to correctly render existing historical AP_PSS/PSS licences
  @field:JsonProperty("PSS")
  val standardConditionsPss: List<StandardConditionPss>,
)
