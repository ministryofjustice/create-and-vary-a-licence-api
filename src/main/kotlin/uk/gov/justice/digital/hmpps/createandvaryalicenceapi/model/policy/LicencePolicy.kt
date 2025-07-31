package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

interface ILicenceCondition {
  val code: String
  val text: String
  val tpl: String?
}

interface IAdditionalCondition :
  ILicenceCondition,
  HasInputs {
  val category: String
  val inputs: List<Input>?
  val type: String?
  val requiresInput: Boolean
  val categoryShort: String?
  val skippable: Boolean?
  override fun getConditionInputs() = inputs
}

interface HasInputs {
  @JsonIgnore
  fun getConditionInputs(): List<Input>?

  // Recursively gather all formatting rules, first top level and then recursively to get any conditional rules
  @JsonIgnore
  fun getFormattingRules(): List<Input> {
    val inputs = this.getConditionInputs() ?: emptyList()
    val options = inputs.flatMap { it.options ?: emptyList() }
    val conditionalInputs =
      options.filter { it.conditional != null }.mapNotNull { it.conditional }.flatMap { it.getFormattingRules() }

    return inputs + conditionalInputs
  }
}

data class ChangeHint(
  val previousCode: String,
  val replacements: List<String>,
)

data class LicencePolicy(
  @field:JsonProperty("version")
  val version: String,
  val standardConditions: StandardConditions,
  val additionalConditions: AdditionalConditions,
  val changeHints: List<ChangeHint> = emptyList(),
) {

  @JsonIgnore
  fun allAdditionalConditions(): Set<IAdditionalCondition> = (this.additionalConditions.pss + this.additionalConditions.ap).toSet()

  @JsonIgnore
  fun allStandardConditions(): Set<ILicenceCondition> = (this.standardConditions.standardConditionsPss + this.standardConditions.standardConditionsAp).toSet()
}

data class AllAdditionalConditions(
  val mappedPolicy: Map<String, Map<String, IAdditionalCondition>>,
) {
  fun getCondition(version: String, code: String): IAdditionalCondition = mappedPolicy[version]?.get(code) ?: error("Unable to find condition config for $code in policy version $version")
}
