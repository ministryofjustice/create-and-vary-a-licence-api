package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.IAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ILicenceCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.getSuggestedReplacements
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.licencePolicyChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isOnOrAfter
import java.time.LocalDate
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
class LicencePolicyService(
  private var policies: List<LicencePolicy> = emptyList(),
  @param:Value("\${progression.model.policy-start-date}")
  @param:DateTimeFormat(pattern = "yyyy-MM-dd")
  private val progressionModelPolicyStartDate: LocalDate,
) {

  init {
    policies = listOf(POLICY_V1_0, POLICY_V2_0, POLICY_V2_1, POLICY_V3_0, POLICY_V4_0)
  }

  fun currentPolicy(licenceStartDate: LocalDate? = null): LicencePolicy {
    if (licenceStartDate?.isOnOrAfter(progressionModelPolicyStartDate) == true) {
      return POLICY_V4_0
    }
    return POLICY_V3_0
  }

  fun policyByVersion(version: String): LicencePolicy = policies.find { it.version == version }
    ?: throw EntityNotFoundException("policy version $version not found")

  fun allPolicies(): List<LicencePolicy> = policies

  fun compareLicenceWithPolicy(
    licence: ModelLicence,
    previousLicence: ModelLicence,
    versionToCompare: String,
  ): List<LicenceConditionChanges> {
    val previousPolicy = previousLicence.version?.let { policyByVersion(it) }
    val currentPolicy = policyByVersion(versionToCompare)

    if (previousPolicy === null || previousPolicy.version == currentPolicy.version) return emptyList()

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

  fun getConfigForCondition(version: String, conditionCode: String): IAdditionalCondition = policyByVersion(version)
    .allAdditionalConditions()
    .find { it.code == conditionCode }
    ?: error("Condition with code: '$conditionCode' and version: '$version' not found.")

  fun getConditionsRequiringElectronicMonitoringResponse(
    version: String,
    conditionCodes: Set<String>,
  ): List<IAdditionalCondition> = policyByVersion(version)
    .additionalConditions.ap
    .filter { it.requiresElectronicMonitoringResponse && conditionCodes.contains(it.code) }

  fun isElectronicMonitoringResponseRequired(conditionCodes: Set<String>, version: String): Boolean {
    val conditionsRequiringResponse = getConditionsRequiringElectronicMonitoringResponse(
      version,
      conditionCodes,
    )
    if (conditionsRequiringResponse.isNotEmpty()) {
      log.info(
        "Handling Electronic Monitoring response record for conditions: ${
          conditionsRequiringResponse.joinToString(
            ",",
          ) { it.code }
        }",
      )
      return true
    }
    return false
  }

  fun getCurrentStandardConditions(licence: Licence) = if (licence.typeCode == PSS) {
    emptyList()
  } else {
    currentPolicy(licence.licenceStartDate).standardConditions.standardConditionsAp
  }

  fun getCurrentPssRequirements(licence: Licence) = if (licence.typeCode == AP) {
    emptyList()
  } else {
    currentPolicy(licence.licenceStartDate).standardConditions.standardConditionsPss
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
      getCurrentStandardConditions(licence).mapIndexed(toEntityStandardCondition(licence, "AP"))
    val pssRequirements =
      getCurrentPssRequirements(licence).mapIndexed(toEntityStandardCondition(licence, "PSS"))

    return standardConditions + pssRequirements
  }

  fun getAllAdditionalConditions(): AllAdditionalConditions = AllAdditionalConditions(
    policies.associate {
      it.version to it.allAdditionalConditions().associateBy { condition -> condition.code }
    },
  )

  fun getHardStopAdditionalConditions(licence: Licence): List<AdditionalCondition> = when {
    licence.typeCode == AP || licence.typeCode == AP_PSS -> listOf(HARD_STOP_CONDITION).mapIndexed(
      toEntityAdditionalCondition(
        licence,
        "AP",
      ),
    )

    else -> emptyList()
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
