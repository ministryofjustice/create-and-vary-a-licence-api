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
fun getLicenceConditionStatuses(
  licenceConditions: List<AdditionalCondition>,
  policyConditions: AllAdditionalConditions,
): Map<String, ConditionStatus> {
  val conditionsWithConfig = mapConditionsToConfig(licenceConditions, policyConditions)

  return conditionsWithConfig.associate { conditionWithConfig ->
    val condition = conditionWithConfig.additionalCondition
    val config = conditionWithConfig.config

    val requiresInput = config.requiresInput
    val enteredFields = condition.additionalConditionData.map { it.dataField }

    val readyToSubmit = if (!requiresInput) {
      true
    } else {
      val requiredFields = config.getConditionInputs()
        ?.flatMap { it.getAllFieldNames() }
        .orEmpty()

      requiredFields.any { it in enteredFields }
    }

    condition.conditionCode to ConditionStatus(readyToSubmit, requiresInput)
  }
}

fun isConditionReadyToSubmit(
  licenceCondition: AdditionalCondition,
  policyConditions: AllAdditionalConditions,
): ConditionStatus = getLicenceConditionStatuses(listOf(licenceCondition), policyConditions)[licenceCondition.conditionCode]!!
