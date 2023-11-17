package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition

data class AdditionalConditionWithConfig(
  val additionalCondition: AdditionalCondition,
  val config: IAdditionalCondition,
)

fun mapConditionsToConfig(licenceConditions: List<AdditionalCondition>, policy: Set<IAdditionalCondition>): List<AdditionalConditionWithConfig> {
  return licenceConditions.map {
    val policyCondition = policy.find { pc -> it.conditionCode == pc.code }!!
    AdditionalConditionWithConfig(it, policyCondition)
  }
}

fun checkConditionsReadyToSubmit(licenceConditions: List<AdditionalCondition>, policy: Set<IAdditionalCondition>): Map<String, Boolean> {
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

fun checkConditionReadyToSubmit(licenceCondition: AdditionalCondition, policy: Set<IAdditionalCondition>): Boolean {
  return checkConditionsReadyToSubmit(listOf(licenceCondition), policy)[licenceCondition.conditionCode]!!
}
