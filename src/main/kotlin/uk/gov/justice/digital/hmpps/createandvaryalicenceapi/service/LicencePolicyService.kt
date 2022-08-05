package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PolicyConfiguration
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionsAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionsPss
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicencePolicyDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardConditionsAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardConditionsPss
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

data class PolicyChanges(
  val standardConditions: ConditionChangesByType<StandardConditionsAp, StandardConditionsPss>,
  val additionalConditions: ConditionChangesByType<AdditionalConditionsAp, AdditionalConditionsPss>
)

@Service
class LicencePolicyService(
  private val policies: PolicyConfiguration
) {
  fun currentPolicy(): LicencePolicyDto = policies.currentPolicy
  fun policyByVersion(version: String): LicencePolicyDto = policies.policies().find { it.version == version }
    ?: throw EntityNotFoundException("policy version $version not found")

  fun allPolicies(): List<LicencePolicyDto> = policies.policies()
  fun compare(version1: LicencePolicyDto, version2: LicencePolicyDto): PolicyChanges {
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
