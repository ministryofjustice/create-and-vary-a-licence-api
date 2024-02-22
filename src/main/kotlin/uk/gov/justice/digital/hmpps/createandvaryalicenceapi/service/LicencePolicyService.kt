package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ILicenceCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.HARD_STOP_CONDITION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V1_0
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_0
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as ModelLicence

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
  TEXT_CHANGE,
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
  val suggestions: List<SuggestedCondition> = emptyList(),
)

@Service
class LicencePolicyService(private val policies: List<LicencePolicy> = listOf(POLICY_V1_0, POLICY_V2_0, POLICY_V2_1)) {

  fun currentPolicy(): LicencePolicy = policies.maxBy { it.version }
  fun policyByVersion(version: String): LicencePolicy = policies.find { it.version == version }
    ?: throw EntityNotFoundException("policy version $version not found")

  fun allPolicies(): List<LicencePolicy> = policies

  fun compareLicenceWithPolicy(licence: ModelLicence, previousPolicy: LicencePolicy, currentPolicy: LicencePolicy): List<LicenceConditionChanges> {
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

  fun getConfigForCondition(version: String, conditionCode: String): IAdditionalCondition =
    policyByVersion(version)
      .allAdditionalConditions()
      .find { it.code == conditionCode }
      ?: error("Condition with code: '$conditionCode' and version: '$version' not found.")

  fun getCurrentStandardConditions(licenceType: LicenceType) = if (licenceType == PSS) {
    emptyList()
  } else {
    currentPolicy().standardConditions.standardConditionsAp
  }

  fun getCurrentPssRequirements(licenceType: LicenceType) = if (licenceType == AP) {
    emptyList()
  } else {
    currentPolicy().standardConditions.standardConditionsPss
  }

  private fun toEntityStandardCondition(licence: Licence, type: String) = { i: Int, condition: ILicenceCondition ->
    StandardCondition(
      licence = licence,
      conditionType = type,
      conditionSequence = i,
      conditionCode = condition.code,
      conditionText = condition.text,
    )
  }

  private fun toEntityAdditionalCondition(licence: Licence, type: String) = { i: Int, condition: IAdditionalCondition ->
    AdditionalCondition(
      licence = licence,
      conditionType = type,
      conditionSequence = i,
      conditionCode = condition.code,
      conditionText = condition.text,
      expandedConditionText = condition.text,
      conditionVersion = licence.version!!,
      conditionCategory = condition.categoryShort ?: condition.category,
    )
  }

  fun getStandardConditionsForLicence(licence: Licence): List<StandardCondition> {
    val standardConditions =
      getCurrentStandardConditions(licence.typeCode).mapIndexed(toEntityStandardCondition(licence, "AP"))
    val pssRequirements =
      getCurrentPssRequirements(licence.typeCode).mapIndexed(toEntityStandardCondition(licence, "PSS"))

    return standardConditions + pssRequirements
  }

  fun getAllAdditionalConditions(): AllAdditionalConditions {
    return AllAdditionalConditions(
      policies.associate {
        it.version to it.allAdditionalConditions().associateBy { condition -> condition.code }
      },
    )
  }

  fun getHardStopAdditionalConditions(licence: Licence): List<AdditionalCondition> =
    when {
      licence.typeCode == AP || licence.typeCode == AP_PSS -> listOf(HARD_STOP_CONDITION).mapIndexed(
        toEntityAdditionalCondition(
          licence,
          "AP",
        ),
      )

      else -> emptyList()
    }
}
