package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements

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
  @JsonIgnore var addedInputs: List<Any>,
  @JsonIgnore var removedInputs: List<Any>,
  val suggestions: List<SuggestedCondition> = emptyList()
)

class LicencePolicyService(private val policies: List<LicencePolicy>) {

  fun currentPolicy(): LicencePolicy = policies.maxBy { it.version }
  fun policyByVersion(version: String): LicencePolicy = policies.find { it.version == version }
    ?: throw EntityNotFoundException("policy version $version not found")

  fun allPolicies(): List<LicencePolicy> = policies

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
