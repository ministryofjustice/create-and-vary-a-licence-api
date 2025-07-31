package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class StandardConditions(
  @field:JsonProperty("AP")
  val standardConditionsAp: List<StandardConditionAp>,
  @field:JsonProperty("PSS")
  val standardConditionsPss: List<StandardConditionPss>,
)
