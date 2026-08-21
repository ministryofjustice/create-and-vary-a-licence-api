package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import com.fasterxml.jackson.annotation.JsonIgnore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditionChanges
import java.time.LocalDate
import kotlin.math.max
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

    allPolicies.map { it.version }.sorted().indices.flatMap { i ->
      (i + 1 until allPolicies.map { it.version }.sorted().size).map { j ->
        allPolicies.map { it.version }.sorted()[i] to allPolicies.map { it.version }.sorted()[j]
      }
    }.forEach { (version1, version2) ->
      val version1Policy = licencePolicyService.policyByVersion(version1)
      val version2Policy = licencePolicyService.policyByVersion(version2)

      val replacements = getSuggestedReplacements(version1Policy, version2Policy)

      val changesFromPolicyV1ToV2 =
        conditionChanges(version1Policy.additionalConditions.ap, version2Policy.additionalConditions.ap, replacements)

      val policyChanges = conditionChangesToPolicyChanges(version1, version2, changesFromPolicyV1ToV2)
      allPolicyVersionChanges.add(policyChanges)
    }

    val expectedDiffJson = readFile("allPolicyVersionChanges")
    val actualDiffJson = diffsAsJson(allPolicyVersionChanges)

    // compareJson(expectedDiffJson, actualDiffJson)
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

  private fun compareJson(expectedJson: String, actualJson: String) {
    val expectedNode = objectMapper.readTree(expectedJson)
    val actualNode = objectMapper.readTree(actualJson)
    // assertEquals(expectedNode, actualNode, "JSON nodes are not equal")
    compareJsonNodes(expectedNode, actualNode)
  }

  fun compareJsonNodes(node1: JsonNode, node2: JsonNode, path: String = "") {
    if (node1 == node2) return

    if (node1.nodeType != node2.nodeType) {
      println("Difference at $path: Type mismatch (${node1.nodeType} vs ${node2.nodeType})")
      return
    }

    when {
      node1.isObject -> {
        // Check fields in node1 and compare with node2
        node1.properties().forEach { (fieldName, child1) ->
          val currentPath = if (path.isEmpty()) fieldName else "$path/$fieldName"
          val child2 = node2.get(fieldName)

          if (child2 == null) {
            println("Difference at $currentPath: Missing in second node")
          } else {
            compareJsonNodes(child1, child2, currentPath)
          }
        }

        // Check for fields present in node2 but missing in node1
        node2.propertyNames().forEach { fieldName ->
          if (!node1.has(fieldName)) {
            val currentPath = if (path.isEmpty()) fieldName else "$path/$fieldName"
            println("Difference at $currentPath: Missing in first node")
          }
        }
      }

      node1.isArray -> {
        val arr1 = node1 as ArrayNode
        val arr2 = node2 as ArrayNode
        val maxSize = max(arr1.size(), arr2.size())

        for (i in 0 until maxSize) {
          val currentPath = "$path[$i]"
          when {
            i >= arr1.size() -> println("Difference at $currentPath: Extra element in second array")
            i >= arr2.size() -> println("Difference at $currentPath: Missing element in second array")
            else -> compareJsonNodes(arr1[i], arr2[i], currentPath)
          }
        }
      }

      else -> {
        println("Difference at $path: '${node1.asString()}' vs '${node2.asString()}'")
      }
    }
  }

  fun readFile(filename: String): String = this.javaClass.getResourceAsStream("/test_data/policy_conditions/$filename.json")!!.bufferedReader(UTF_8).readText()
}
