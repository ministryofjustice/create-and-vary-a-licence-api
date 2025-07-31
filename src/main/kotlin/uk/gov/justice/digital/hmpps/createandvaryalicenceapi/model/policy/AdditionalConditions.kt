package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class AdditionalConditions(
  @field:JsonProperty("AP")
  val ap: List<AdditionalConditionAp>,
  @field:JsonProperty("PSS")
  val pss: List<AdditionalConditionPss>,
)
