package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition

data class AdditionalConditionWithConfig(
  val additionalCondition: AdditionalCondition,
  val config: IAdditionalCondition,
)

data class ConditionStatus(
  val readyToSubmit: Boolean,
  val requiresInput: Boolean,
)

fun mapConditionsToConfig(
  licenceConditions: List<AdditionalCondition>,
  policyConditions: AllAdditionalConditions,
): List<AdditionalConditionWithConfig> = licenceConditions.map {
  val policyCondition = policyConditions.getCondition(it.conditionVersion, it.conditionCode)
  AdditionalConditionWithConfig(it, policyCondition)
}

/*
  A condition is deemed ready to submit if it has data for all required inputs.
  Data-input is all-or-nothing (ie the user cannot submit data without filling out all required fields),
  so we can infer that the presence of any data means all the required data exists and the condition is ready to submit.
*/
// fun isLicenceReadyToSubmit(
//  licenceConditions: List<AdditionalCondition>,
//  policyConditions: AllAdditionalConditions,
// ): Map<String, Boolean> {
//  val conditionsWithConfig = mapConditionsToConfig(licenceConditions, policyConditions)
//  return conditionsWithConfig.associate {
//    val enteredFields = it.additionalCondition.additionalConditionData.map { data -> data.dataField }
//    val readyToSubmit =
//      if (!it.config.requiresInput) {
//        true
//      } else {
//        val policyInputs = it.config.getConditionInputs()!!.flatMap { input -> input.getAllFieldNames() }
//        policyInputs.any { name -> enteredFields.contains(name) }
//      }
//    Pair(it.additionalCondition.conditionCode, readyToSubmit)
//  }
// }

fun getLicenceConditionStatuses(
  licenceConditions: List<AdditionalCondition>,
  policyConditions: AllAdditionalConditions,
): Map<String, ConditionStatus> {
  val conditionsWithConfig = mapConditionsToConfig(licenceConditions, policyConditions)
  return conditionsWithConfig.associate {
    val enteredFields = it.additionalCondition.additionalConditionData.map { data -> data.dataField }
    val readyToSubmit =
      if (!it.config.requiresInput) {
        true
      } else {
        val policyInputs = it.config.getConditionInputs()!!.flatMap { input -> input.getAllFieldNames() }
        policyInputs.any { name -> enteredFields.contains(name) }
      }
    Pair(it.additionalCondition.conditionCode, ConditionStatus(readyToSubmit, it.config.requiresInput))
  }
}

fun isConditionReadyToSubmit(
  licenceCondition: AdditionalCondition,
  policyConditions: AllAdditionalConditions,
): ConditionStatus = getLicenceConditionStatuses(listOf(licenceCondition), policyConditions)[licenceCondition.conditionCode]!!
