package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ILicenceCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Replacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.TEXT_CHANGE

/**
 * This will return a list of all changes that need to be addressed as part of migrating a licence from one version of the policy to another.
 */
fun <T : Any> licencePolicyChanges(
  licence: Licence,
  previousConditions: List<IAdditionalCondition<T>>,
  currentConditions: List<IAdditionalCondition<T>>,
  allReplacements: List<Replacements>,
): List<LicenceConditionChanges> {
  val conditionsToCheck = previousConditions.filter { policyCondition ->
    licence.additionalLicenceConditions.any { licenceCondition ->
      licenceCondition.code == policyCondition.code && licence.version != licenceCondition.version
    }
  }
  val changes = conditionChanges(conditionsToCheck, currentConditions, allReplacements)

  return changes.map { change ->
    change.copy(
      sequence = licence.additionalLicenceConditions.find { condition -> condition.code == change.code }?.sequence,
    )
  }
}

fun <T : Any> conditionChanges(
  previousConditions: List<IAdditionalCondition<T>>,
  currentConditions: List<IAdditionalCondition<T>>,
  allReplacements: List<Replacements>,
): List<LicenceConditionChanges> = previousConditions.mapNotNull { previous ->
  val current = currentConditions.find { it.code == previous.code }
  if (current != null) {
    conditionUpdated(previous, current)
  } else {
    conditionRemoved(previous, allReplacements)
  }
}

private fun <T : Any> conditionUpdated(
  previous: IAdditionalCondition<T>,
  current: IAdditionalCondition<T>,
): LicenceConditionChanges? {
  val textHasChanged = previous.text != current.text
  val removed = (previous.inputs ?: emptyList()) - (current.inputs ?: emptyList()).toSet()
  val added = (current.inputs ?: emptyList()) - (previous.inputs ?: emptyList()).toSet()
  val dataHasChanged = removed.isNotEmpty() || added.isNotEmpty()
  return when {
    textHasChanged -> TEXT_CHANGE.update(previous, current)
    dataHasChanged -> NEW_OPTIONS.update(previous, current, removed, added)
    else -> null
  }
}

private fun conditionRemoved(
  condition: ILicenceCondition,
  allReplacements: List<Replacements>,
): LicenceConditionChanges {
  val replacements = allReplacements.find { it.code === condition.code }
  return when (replacements?.changeType) {
    DELETED -> DELETED.removal(condition, replacements.alternatives)
    REPLACED -> REPLACED.removal(condition, replacements.alternatives)
    else -> REMOVED_NO_REPLACEMENTS.removal(condition, emptyList())
  }
}

fun ConditionChangeType.update(
  previous: ILicenceCondition,
  current: ILicenceCondition,
  removed: List<Any> = emptyList(),
  added: List<Any> = emptyList(),
): LicenceConditionChanges =
  LicenceConditionChanges(this, current.code, null, previous.text, current.text, added, removed)

fun ConditionChangeType.removal(
  condition: ILicenceCondition,
  alternatives: List<ILicenceCondition>,
) = LicenceConditionChanges(
  this,
  condition.code,
  null,
  condition.text,
  null,
  emptyList(),
  emptyList(),
  alternatives.map { SuggestedCondition(it.code, it.text) },
)
