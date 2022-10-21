package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ILicenceCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Replacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.TEXT_CHANGE

fun <T : ILicenceCondition> removedConditions(current: List<T>, other: List<T>): List<T> =
  current.filter { pssElement ->
    !other.any { pss -> pss.code == pssElement.code }
  }

fun <T : ILicenceCondition> addedConditions(current: List<T>, other: List<T>): List<T> =
  other.filter { pssElement ->
    !current.any { pss -> pss.code == pssElement.code }
  }

fun <T : ILicenceCondition> amendedConditions(
  current: List<T>,
  other: List<T>
): List<Pair<T, T>> =
  current.mapNotNull { c -> other.find { it.code == c.code && it != c }?.let { Pair(c, it) } }

fun <T : ILicenceCondition> conditionChanges(current: List<T>, other: List<T>) = ConditionChanges(
  removedConditions(current, other),
  addedConditions(current, other),
  amendedConditions(current, other)
)

fun <T : Any> licencePolicyChanges(
  presentConditions: List<AdditionalCondition>,
  previousConditions: List<IAdditionalCondition<T>>,
  currentConditions: List<IAdditionalCondition<T>>,
  allReplacements: List<Replacements>
): List<LicenceConditionChanges> {
  val conditionsToCheck = currentConditions.filter { current ->
    presentConditions.any { present -> current.code == present.code }
  }
  val changes = conditionChanges(conditionsToCheck, previousConditions, allReplacements)

  return changes.map { change -> change.copy(sequence = presentConditions.find { condition -> condition.code == change.code }?.sequence) }
}

fun <T : Any> conditionChanges(
  previousConditions: List<IAdditionalCondition<T>>,
  currentConditions: List<IAdditionalCondition<T>>,
  allReplacements: List<Replacements>
): List<LicenceConditionChanges> = previousConditions.mapNotNull { previous ->
  val current = currentConditions.find { it.code == previous.code }
  if (current != null)
    conditionUpdated(previous, current)
  else
    conditionRemoved(previous, allReplacements)
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
  added: List<Any> = emptyList()
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
  alternatives.map { SuggestedCondition(it.code, it.text) }
)
