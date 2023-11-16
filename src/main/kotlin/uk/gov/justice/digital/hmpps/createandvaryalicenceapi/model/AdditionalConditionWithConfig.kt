package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition

data class AdditionalConditionWithConfig(
  val additionalCondition: AdditionalCondition,
  val config: IAdditionalCondition,
)

fun mapConditionsToConfig(licenceConditions: List<AdditionalCondition>, policy: AdditionalConditions): List<AdditionalConditionWithConfig> {
  val policyAdditionalConditions = policy.ap + policy.pss
  return licenceConditions.map {
    val policyCondition = policyAdditionalConditions.find { pc -> it.conditionCode == pc.code }!!
    AdditionalConditionWithConfig(it, policyCondition)
  }
}

fun checkConditionsReadyToSubmit(licenceConditions: List<AdditionalCondition>, policy: AdditionalConditions): Map<String, Boolean> {
  if (licenceConditions.isEmpty()) { return emptyMap() }
  val conditionsWithConfig = mapConditionsToConfig(licenceConditions, policy)
  return conditionsWithConfig.associate {
    val enteredFields = it.additionalCondition.additionalConditionData.map { data -> data.dataField }
    val inputEntered =
      if (!it.config.requiresInput) {
        true
      } else {
        it.config.getConditionInputs()!!.map { input -> input.name }.any { name -> enteredFields.contains(name) }
      }
    Pair(it.additionalCondition.conditionCode!!, inputEntered)
  }
}
