package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.PolicyChanges
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
      version1.standardConditions.standardConditionsPsses,
      version2.standardConditions.standardConditionsPsses
    )

    val additionalApChanges = conditionChanges(version1.additionalConditions.ap, version2.additionalConditions.ap)
    val additionalPssChanges = conditionChanges(version1.additionalConditions.pss, version2.additionalConditions.pss)

    return PolicyChanges(
      standardConditions = ConditionChangesByType(stdApChanges, stdPssChanges),
      additionalConditions = ConditionChangesByType(additionalApChanges, additionalPssChanges)
    )
  }
}
