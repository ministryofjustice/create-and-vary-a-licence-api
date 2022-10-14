package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ILicenceCondition

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
fun <T : ILicenceCondition> getPolicyPlaceholders(conditions: List<T>): HashMap<String, HashMap<String, String>> {
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
  policyPlaceholders: HashMap<String, HashMap<String, String>>
): List<LicenceConditionChanges> = conditions.mapNotNull { c ->
  policyConditions.find { it.code == c.code }?.let { pc ->
    val textHasChanged = pc.text != c.text
    val dataChanges = c.data.filter { d -> policyPlaceholders[c.code]?.containsValue(d.field) == false }
    if (textHasChanged || dataChanges.isNotEmpty()) LicenceConditionChanges(
      c.code!!, c.sequence, c.text!!, pc.text, dataChanges
    )
    else null
  }
}
