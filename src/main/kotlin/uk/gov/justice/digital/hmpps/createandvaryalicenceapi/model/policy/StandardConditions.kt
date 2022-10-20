package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class StandardConditions(
  @JsonProperty("AP")
  val standardConditionsAp: List<StandardConditionAp>,
  @JsonProperty("PSS")
  val standardConditionsPss: List<StandardConditionPss>,
)
