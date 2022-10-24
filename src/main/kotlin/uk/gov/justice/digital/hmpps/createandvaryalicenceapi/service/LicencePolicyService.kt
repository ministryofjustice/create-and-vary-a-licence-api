package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.PolicyChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import javax.persistence.EntityNotFoundException

data class ConditionChanges<T>(
  val removedConditions: List<T>,
  val addedConditions: List<T>,
  val amendedConditions: List<Pair<T, T>>
)

data class ConditionChangesByType<T, U>(
  val Ap: ConditionChanges<T>,
  val Pss: ConditionChanges<U>
)

enum class ConditionChangeType {
  /**
   * Deleted conditions are those whose replacements contain one or more existing conditions,
   * in other words, the replacements are suggestions of existing conditions that may be appropriate, or may not.
   */
  DELETED,

  /**
   * Replaced conditions are those whose replacements are all new conditions,
   * in other words, the replacements have been added as explicit replacements for the deleted condition.
   */
  REPLACED,

  REMOVED_NO_REPLACEMENTS,
  NEW_OPTIONS,
  TEXT_CHANGE
}

data class SuggestedCondition(
  val code: String,
  val currentText: String,
)

data class LicenceConditionChanges(
  val changeType: ConditionChangeType,
  val code: String,
  val sequence: Int?,
  val previousText: String,
  val currentText: String?,
  var addedInputs: List<Any>,
  var removedInputs: List<Any>,
  val suggestions: List<SuggestedCondition> = emptyList()
)

class LicencePolicyService(private val policies: List<LicencePolicy>) {

  fun currentPolicy(): LicencePolicy = policies.maxBy { it.version }
  fun policyByVersion(version: String): LicencePolicy = policies.find { it.version == version }
    ?: throw EntityNotFoundException("policy version $version not found")

  fun allPolicies(): List<LicencePolicy> = policies
  fun compare(version1: LicencePolicy, version2: LicencePolicy): PolicyChanges {
    val stdApChanges = conditionChanges(
      version1.standardConditions.standardConditionsAp,
      version2.standardConditions.standardConditionsAp
    )
    val stdPssChanges = conditionChanges(
      version1.standardConditions.standardConditionsPss,
      version2.standardConditions.standardConditionsPss
    )

    val additionalApChanges = conditionChanges(version1.additionalConditions.ap, version2.additionalConditions.ap)
    val additionalPssChanges = conditionChanges(version1.additionalConditions.pss, version2.additionalConditions.pss)

    return PolicyChanges(
      standardConditions = ConditionChangesByType(stdApChanges, stdPssChanges),
      additionalConditions = ConditionChangesByType(additionalApChanges, additionalPssChanges)
    )
  }

  fun compareLicenceWithPolicy(licence: Licence, previousPolicy: LicencePolicy, currentPolicy: LicencePolicy):

    List<LicenceConditionChanges> {
    if (previousPolicy.version == currentPolicy.version) return emptyList()
    val replacements = getSuggestedReplacements(previousPolicy, currentPolicy)
    return licencePolicyChanges(
      licence,
      previousPolicy.additionalConditions.ap,
      currentPolicy.additionalConditions.ap,
      replacements,
    ) + licencePolicyChanges(
      licence,
      previousPolicy.additionalConditions.pss,
      currentPolicy.additionalConditions.pss,
      replacements,
    )
  }
}
