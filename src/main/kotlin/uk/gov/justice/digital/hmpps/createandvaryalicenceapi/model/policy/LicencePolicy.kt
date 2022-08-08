package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonProperty

interface ILicenceCondition {
  var code: String
}

data class LicencePolicy(
  @JsonProperty("version")
  val version: String,
  val standardConditions: StandardConditions,
  val additionalConditions: AdditionalConditions,
)
