package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.DELETED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.NEW_OPTIONS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REMOVED_NO_REPLACEMENTS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.REPLACED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ConditionChangeType.TEXT_CHANGE

class RealLicencePolicyServiceTest() {

  private val licencePolicyService = LicencePolicyService(
    listOf(
      ClassPathResource("/policy_conditions/policyV2.json").file,
      ClassPathResource("/policy_conditions/policyV2.1.json").file
    ).map { policy -> jacksonObjectMapper().readValue(policy) }
  )

  @Test
  fun `Current policy version is returned`() {
    val currentPolicy = licencePolicyService.policyByVersion("2.1")
    val previousPolicy = licencePolicyService.policyByVersion("2.0")

    val replacements = getSuggestedReplacements(previousPolicy, currentPolicy)

    val diffs =
      conditionChanges(previousPolicy.additionalConditions.ap, currentPolicy.additionalConditions.ap, replacements)

    diffs.forEach {
      when (it.changeType) {
        TEXT_CHANGE ->
          println(
            """
            type:   ${it.changeType}
            id:     ${it.code}
            before: ${it.previousText}
            after:  ${it.currentText}
            """.trimIndent()
          )
        NEW_OPTIONS ->
          println(
            """
            type:   ${it.changeType}
            id:     ${it.code}
            before: ${it.previousText}
            after:  ${it.currentText}
            added: ${it.addedInputs}
            removed:  ${it.removedInputs}
            """.trimIndent()
          )
        REPLACED -> {
          println(
            """
            type:         ${it.changeType}
            id:           ${it.code}
            """.trimIndent()
          )
          it.suggestions.forEach { suggestedCondition ->
            println(" * $suggestedCondition")
          }
        }
        DELETED -> {
          println(
            """
            type:         ${it.changeType}
            id:           ${it.code}
            """.trimIndent()
          )
          it.suggestions.forEach { suggestedCondition ->
            println(" * $suggestedCondition")
          }
        }

        REMOVED_NO_REPLACEMENTS -> println("ERROR!: NO REPLACEMENTS")
      }
      println()
    }
  }
}
