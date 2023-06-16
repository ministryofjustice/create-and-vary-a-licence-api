package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED

data class Replacements(
  val code: String,
  val changeType: ConditionChangeType,
  val alternatives: List<ILicenceCondition>,
)

fun getSuggestedReplacements(previous: LicencePolicy?, current: LicencePolicy): List<Replacements> {
  val previousConditions = previous?.allAdditionalConditions()?.associateBy { it.code } ?: emptyMap()
  val currentConditions = current.allAdditionalConditions().associateBy { it.code }

  return when (previous) {
    null -> emptyList()
    else ->
      previous.allAdditionalConditions()
        .filterNot { currentConditions.containsKey(it.code) }
        .map { condition ->

          val replacements = current.changeHints.find { it.previousCode == condition.code }?.replacements ?: emptyList()

          val type = when {
            replacements.isEmpty() -> REMOVED_NO_REPLACEMENTS
            replacements.any { previousConditions.containsKey(it) } -> DELETED
            else -> REPLACED
          }

          val replacementConditions = replacements.mapNotNull { currentConditions[it] ?: previousConditions[it] }

          Replacements(condition.code, type, replacementConditions)
        }
  }
}
