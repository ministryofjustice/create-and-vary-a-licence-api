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

/*
  A condition is deemed ready to submit if it has data for all required inputs.
  Data-input is all-or-nothing (ie the user cannot submit data without filling out all required fields),
  so we can infer that the presence of any data means all the required data exists and the condition is ready to submit.
*/
fun isLicenceReadyToSubmit(licenceConditions: List<AdditionalCondition>, policy: Set<IAdditionalCondition>): Map<String, Boolean> {
  val conditionsWithConfig = mapConditionsToConfig(licenceConditions, policy)
  return conditionsWithConfig.associate {
    val enteredFields = it.additionalCondition.additionalConditionData.map { data -> data.dataField }
    val readyToSubmit =
      if (!it.config.requiresInput) {
        true
      } else {
        val policyInputs = it.config.getConditionInputs()!!.flatMap { input -> input.getAllFieldNames() }
        policyInputs.any { name -> enteredFields.contains(name) }
      }
    Pair(it.additionalCondition.conditionCode!!, readyToSubmit)
  }
}

fun isConditionReadyToSubmit(licenceCondition: AdditionalCondition, policy: Set<IAdditionalCondition>): Boolean {
  return isLicenceReadyToSubmit(listOf(licenceCondition), policy)[licenceCondition.conditionCode]!!
}
