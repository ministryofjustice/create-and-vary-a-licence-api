package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

interface ILicenceCondition {
  val code: String
  val text: String
  val tpl: String?
}

interface IAdditionalCondition : ILicenceCondition, HasInputs {
  val category: String
  val inputs: List<Input>?
  val type: String?
  val requiresInput: Boolean
  val categoryShort: String?
  override fun getConditionInputs() = inputs
}

interface HasInputs {
  @JsonIgnore
  fun getConditionInputs(): List<Input>?
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
