package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import com.fasterxml.jackson.annotation.JsonIgnore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditionChanges
import java.time.LocalDate
import kotlin.text.Charsets.UTF_8

data class ConditionChanges(
  val changeType: ConditionChangeType,
  val code: String,
  val previousText: String,
  val currentText: String?,
  @JsonIgnore var addedInputs: List<Any>,
  @JsonIgnore var removedInputs: List<Any>,
  val suggestions: List<SuggestedCondition> = emptyList(),
)

data class PolicyDifferences(
  val fromVersion: String,
  val toVersion: String,
  val changes: List<ConditionChanges>,
)

class PolicyMigrationTest {
  private val currentPolicy = POLICY_V4_0.version

  private val licencePolicyService = LicencePolicyService(progressionModelPolicyStartDate = LocalDate.now())
  private val objectMapper = jacksonObjectMapper()

  @Test
  fun `expected current policy`() {
    val latestPolicy = licencePolicyService.allPolicies().maxOfOrNull { it.version }
    assertEquals(latestPolicy, currentPolicy, "PolicyMigrationTest must be updated for the lastest policy.")
  }

  @Test
  fun `check the changes between different policy versions are as expected`() {
    val allPolicies = licencePolicyService.allPolicies()
    val allPolicyVersionChanges = mutableListOf<PolicyDifferences>()

    val versions = allPolicies.map { it.version }.sorted()
    versions.flatMapIndexed { i, version1 ->
      versions.drop(i + 1).map { version2 -> version1 to version2 }
    }.forEach { (version1, version2) ->
      val version1Policy = licencePolicyService.policyByVersion(version1)
      val version2Policy = licencePolicyService.policyByVersion(version2)

      val replacements = getSuggestedReplacements(version1Policy, version2Policy)

      val changesFromPolicyV1ToV2 =
        conditionChanges(version1Policy.additionalConditions.ap, version2Policy.additionalConditions.ap, replacements)

      val policyChanges = conditionChangesToPolicyChanges(version1, version2, changesFromPolicyV1ToV2)
      allPolicyVersionChanges.add(policyChanges)
    }

    val expectedDiffJson = readFile("allPolicyVersionChanges").trim()
    val actualDiffJson = diffsAsJson(allPolicyVersionChanges)

    assertEquals(expectedDiffJson, actualDiffJson)
  }

  private fun conditionChangesToPolicyChanges(
    fromVersion: String,
    toVersion: String,
    changesV1ToV2: List<LicenceConditionChanges>,
  ): PolicyDifferences {
    val conditionChanges = changesV1ToV2.map {
      ConditionChanges(
        changeType = it.changeType,
        code = it.code,
        previousText = it.previousText,
        currentText = it.currentText,
        addedInputs = it.addedInputs,
        removedInputs = it.removedInputs,
        suggestions = it.suggestions,
      )
    }

    return PolicyDifferences(
      fromVersion = fromVersion,
      toVersion = toVersion,
      changes = conditionChanges,
    )
  }

  private fun diffsAsJson(diffs: List<PolicyDifferences>): String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(diffs)

  fun readFile(filename: String): String = this.javaClass.getResourceAsStream("/test_data/policy_conditions/$filename.json")!!.bufferedReader(UTF_8).readText()
}
