package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

data class AdditionalConditions(
  @field:JsonProperty("AP")
  val ap: List<AdditionalConditionAp>,
  // PSS has been repealed - these are retained only to correctly render/vary existing historical PSS licences
  @field:JsonProperty("PSS")
  val pss: List<AdditionalConditionPss>,
)
