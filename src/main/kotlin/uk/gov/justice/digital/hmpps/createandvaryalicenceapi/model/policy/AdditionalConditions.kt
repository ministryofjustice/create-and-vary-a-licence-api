package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class AdditionalConditions(
  @JsonProperty("AP")
  val ap: List<AdditionalConditionAp>,
  @JsonProperty("PSS")
  val pss: List<AdditionalConditionPss>,
)
