package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditionChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ConditionChangeType.REPLACED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.ConditionChangeType.TEXT_CHANGE

// This doesn't actually assert anything but dumps the differences between different versions of the policy
class PolicyVersionDifferencesTest {

  private val licencePolicyService = LicencePolicyService()

  @Test
  fun `1_0 to 2_0`() {
    val currentPolicy = licencePolicyService.policyByVersion("2.0")
    val previousPolicy = licencePolicyService.policyByVersion("1.0")

    val replacements = getSuggestedReplacements(previousPolicy, currentPolicy)

    val diffs =
      conditionChanges(previousPolicy.additionalConditions.ap, currentPolicy.additionalConditions.ap, replacements)

    showDiffs(diffs)
  }

  @Test
  fun `1_0 to 2_1`() {
    val currentPolicy = licencePolicyService.policyByVersion("2.1")
    val previousPolicy = licencePolicyService.policyByVersion("1.0")

    val replacements = getSuggestedReplacements(previousPolicy, currentPolicy)

    val diffs =
      conditionChanges(previousPolicy.additionalConditions.ap, currentPolicy.additionalConditions.ap, replacements)

    showDiffs(diffs)
  }

  @Test
  fun `2_0 to 2_1`() {
    val currentPolicy = licencePolicyService.policyByVersion("2.1")
    val previousPolicy = licencePolicyService.policyByVersion("2.0")

    val replacements = getSuggestedReplacements(previousPolicy, currentPolicy)

    val diffs =
      conditionChanges(previousPolicy.additionalConditions.ap, currentPolicy.additionalConditions.ap, replacements)

    showDiffs(diffs)
  }

  private fun showDiffs(
    diffs: List<LicenceConditionChanges>,
  ): String {
    var text = ""
    diffs.forEach {
      when (it.changeType) {
        TEXT_CHANGE -> {
          text +=
            """
              type:   ${it.changeType}
              id:     ${it.code}
              before: ${it.previousText}
              after:  ${it.currentText}
            """.trimIndent()
          text += "\n"
        }

        NEW_OPTIONS -> {
          text +=
            """
              type:   ${it.changeType}
              id:     ${it.code}
              before: ${it.previousText}
              after:  ${it.currentText}
              added: ${it.addedInputs}
              removed:  ${it.removedInputs}
            """.trimIndent()
          text += "\n"
        }

        REPLACED -> {
          text +=
            """
              type:   ${it.changeType}
              id:     ${it.code}
            """.trimIndent()
          text += "\n"
          it.suggestions.forEach { suggestedCondition ->
            text += " * $suggestedCondition\n"
          }
        }

        DELETED -> {
          text +=
            """
              type:   ${it.changeType}
              id:     ${it.code}   
            """.trimIndent()
          text += "\n"
          it.suggestions.forEach { suggestedCondition ->
            text += " * $suggestedCondition\n"
          }
        }

        REMOVED_NO_REPLACEMENTS -> println("ERROR!: NO REPLACEMENTS")
      }
      text += "\n"
    }
    println(text)
    return text
  }
}
