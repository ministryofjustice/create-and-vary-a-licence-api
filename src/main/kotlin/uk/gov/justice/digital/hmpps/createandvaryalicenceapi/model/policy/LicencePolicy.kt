package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

interface ILicenceCondition {
  val code: String
  val requiresInput: Boolean
  val text: String
  val tpl: String?
}

interface IAdditionalCondition<INPUT> : ILicenceCondition {
  val category: String
  val inputs: List<INPUT>?
  val type: String?
}

data class ChangeHint(
  val previousCode: String,
  val replacements: List<String>
)

data class LicencePolicy(
  @JsonProperty("version")
  val version: String,
  val standardConditions: StandardConditions,
  val additionalConditions: AdditionalConditions,
  val changeHints: List<ChangeHint> = emptyList()
) {

  @JsonIgnore
  fun allAdditionalConditions(): Set<ILicenceCondition> =
    (this.additionalConditions.pss + this.additionalConditions.ap).toSet()
}
