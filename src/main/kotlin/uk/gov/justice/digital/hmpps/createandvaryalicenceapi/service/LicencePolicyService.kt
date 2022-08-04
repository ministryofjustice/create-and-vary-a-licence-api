package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PolicyConfiguration
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicencePolicyDto

data class ConditionChanges<T>(
  val removedConditions: List<T>,
  val addedConditions: List<T>,
  val amendedConditions: List<Pair<T, T>>
)

data class PolicyChanges<T, U>(
  val standardConditions: ConditionChanges<T>,
  val additionalConditions: ConditionChanges<U>
)

@Service
class LicencePolicyService(
  private val policies: PolicyConfiguration
) {
  fun policyByVersion(version: String): LicencePolicyDto? = policies.policies().find { it.version == version }
  fun allPolicies(): List<LicencePolicyDto> = policies.policies()
}
