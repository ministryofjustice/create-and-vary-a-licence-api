package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionData
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

/*
 *  For each condition, create List item of custom type
 *  Add new list of HashMap<placeholder, variable> to condition list
 *  Iterate through placeholders and add to HashMap with corresponding date field ({} from tpl)
 */
fun <T : ILicenceCondition> getPolicyPlaceholders(conditions: List<T>): Map<String, Map<String, String>> {
  val placeholderPattern = Regex("(?<=\\[)(.*?)(?=])")
  val variablePattern = Regex("(?<=\\{)(.*?)(?=})")
  val results = HashMap<String, HashMap<String, String>>()

  conditions.filter { it.requiresInput }.forEach {
    val placeholders = placeholderPattern.findAll(it.text).iterator()
    val variables = variablePattern.findAll(it.tpl ?: "").toSet().iterator()
    val keyValuePairs = HashMap<String, String>()

    while (placeholders.hasNext() && variables.hasNext()) {
      keyValuePairs[placeholders.next().value] = variables.next().value
    }

    results[it.code] = keyValuePairs
  }

  return results
}

fun <T : ILicenceCondition> licencePolicyChanges(
  conditions: List<AdditionalCondition>,
  policyConditions: List<T>,
  allReplacements: List<Replacements>,
  policyPlaceholders: Map<String, Map<String, String>>
): List<LicenceConditionChanges> = conditions.mapNotNull { condition ->
  val match = policyConditions.find { it.code == condition.code }
  if (match != null)
    conditionUpdated(match, condition, policyPlaceholders)
  else
    conditionRemoved(condition, allReplacements)
}

private fun conditionUpdated(
  match: ILicenceCondition,
  condition: AdditionalCondition,
  policyPlaceholders: Map<String, Map<String, String>>
): LicenceConditionChanges? {
  val textHasChanged = match.text != condition.text
  val dataChanges = condition.data.filter { d -> policyPlaceholders[condition.code]?.containsValue(d.field) == false }
  val dataHasChanged = dataChanges.isNotEmpty()
  return when {
    textHasChanged -> TEXT_CHANGE.update(condition, match.text, dataChanges)
    dataHasChanged -> NEW_OPTIONS.update(condition, match.text, dataChanges)
    else -> null
  }
}

private fun conditionRemoved(
  condition: AdditionalCondition,
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
  condition: AdditionalCondition,
  currentText: String? = null,
  dataChanges: List<AdditionalConditionData> = emptyList()
): LicenceConditionChanges =
  LicenceConditionChanges(this, condition.code!!, condition.sequence, condition.text!!, currentText, dataChanges)

fun ConditionChangeType.removal(
  condition: AdditionalCondition,
  alternatives: List<ILicenceCondition>,
): LicenceConditionChanges =
  LicenceConditionChanges(
    this,
    condition.code!!,
    condition.sequence,
    condition.text!!,
    null,
    emptyList(),
    alternatives.map { SuggestedCondition(it.code, it.text) }
  )
