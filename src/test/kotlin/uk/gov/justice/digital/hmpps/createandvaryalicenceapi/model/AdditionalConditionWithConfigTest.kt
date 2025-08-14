package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AdditionalConditionAp
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.AllAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Conditional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.ConditionalInput
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Input
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.InputType.TEXT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy.Option
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.AdditionalConditionWithConfig
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.isConditionReadyToSubmit
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.isLicenceReadyToSubmit
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.mapConditionsToConfig

class AdditionalConditionWithConfigTest {

  @Nested
  inner class MapConditionsToConfig {
    @Test
    fun `maps conditions and policy together`() {
      assertThat(
        mapConditionsToConfig(
          listOf(anAdditionalConditionEntity),
          aMappedPolicy,
        ),
      ).containsExactly(
        AdditionalConditionWithConfig(anAdditionalConditionEntity, policyApCondition),
      )
    }
  }

  @Nested
  inner class CheckConditionsReadyToSubmit {
    @Test
    fun `returns if there are no conditions on the licence`() {
      assertThat(
        isLicenceReadyToSubmit(
          emptyList(),
          aMappedPolicy,
        ),
      ).isEqualTo(
        emptyMap<String, Boolean>(),
      )
    }

    @Test
    fun `maps conditions with inputs to true`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(anAdditionalConditionEntity, anAdditionalConditionEntity.copy(conditionCode = "code2")),
          aMappedPolicy,
        ),
      ).isEqualTo(
        mapOf(
          "code" to true,
          "code2" to true,
        ),
      )
    }

    @Test
    fun `maps conditions with missing inputs to false`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(
            anAdditionalConditionEntity.copy(additionalConditionData = mutableListOf()),
            anAdditionalConditionEntity.copy(conditionCode = "code2", additionalConditionData = mutableListOf()),
          ),
          aMappedPolicy,
        ),
      ).isEqualTo(
        mapOf(
          "code" to false,
          "code2" to false,
        ),
      )
    }

    @Test
    fun `maps conditions that do not need inputs to true`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(anAdditionalConditionEntity.copy(additionalConditionData = mutableListOf())),
          aMappedPolicyWithoutInputs,
        ),
      ).isEqualTo(
        mapOf(
          "code" to true,
        ),
      )
    }

    @Test
    fun `maps conditions that have optional fields based on the presence of required fields`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(anAdditionalConditionEntity),
          aMappedPolicyWithMultipleInputs,
        ),
      ).isEqualTo(
        mapOf(
          "code" to true,
        ),
      )
    }

    @Test
    fun `when a condition changes between policy versions, checks the policy version that matches the condition version`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(anAdditionalConditionEntity.copy(conditionCode = "code2", additionalConditionData = mutableListOf())),
          aMappedPolicyWithMultipleVersions,
        ),
      ).isEqualTo(
        mapOf(
          "code2" to true,
        ),
      )
    }

    @Test
    fun `checks submission status based on condition policy version, not licence policy version`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(
            anAdditionalConditionEntity.copy(
              conditionVersion = "2.1",
              licence = aLicenceEntity.copy(version = "2.0"),
            ),
          ),
          aMappedPolicyWithMultipleVersions,
        ),
      ).isEqualTo(
        mapOf("code" to true),
      )
    }

    @Test
    fun `checks for conditional inputs when determining submission status`() {
      assertThat(
        isLicenceReadyToSubmit(
          listOf(anAdditionalConditionEntity.copy(additionalConditionData = mutableListOf(conditionalAdditionalConditionData))),
          aMappedPolicyWithConditionalInputs,
        ),
      ).isEqualTo(
        mapOf(
          "code" to true,
        ),
      )
    }
  }

  @Nested
  inner class CheckConditionReadyToSubmit {
    @Test
    fun `returns true for a condition that has inputs`() {
      assertThat(
        isConditionReadyToSubmit(
          anAdditionalConditionEntity,
          aMappedPolicy,
        ),
      ).isTrue()
    }

    @Test
    fun `returns false for a condition that is missing inputs`() {
      assertThat(
        isConditionReadyToSubmit(
          anAdditionalConditionEntity.copy(additionalConditionData = mutableListOf()),
          aMappedPolicy,
        ),
      ).isFalse()
    }

    @Test
    fun `returns true for a condition that doesn't need inputs`() {
      assertThat(
        isConditionReadyToSubmit(
          anAdditionalConditionEntity.copy(additionalConditionData = mutableListOf()),
          aMappedPolicyWithoutInputs,
        ),
      ).isTrue()
    }
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence()

    val anInput = Input(
      type = TEXT,
      label = "Label",
      name = "name",
    )

    val anInputWithConditionalInputs = Input(
      type = TEXT,
      label = "Label",
      name = "name",
      options = listOf(
        Option(
          value = "value",
          conditional = Conditional(
            inputs = listOf(
              ConditionalInput(
                type = TEXT,
                label = "Label",
                name = "conditionalName",
              ),
            ),

          ),
        ),
      ),
    )

    val policyApCondition = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      inputs = listOf(anInput),
      requiresInput = true,
    )

    val policyConditionWithoutInput = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      requiresInput = false,
    )

    val aPolicyConditionWithMultipleInputs = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      inputs = listOf(
        anInput,
        anInput.copy(label = "Label (Optional)"),
      ),
      requiresInput = true,
    )

    val aPolicyConditionWithConditionalInputs = AdditionalConditionAp(
      code = "code",
      category = "category",
      text = "text",
      inputs = listOf(
        anInputWithConditionalInputs,
      ),
      requiresInput = true,
    )

    val someAdditionalConditionData = AdditionalConditionData(
      additionalCondition = anAdditionalCondition(id = 1, licence = aLicenceEntity),
      dataField = "name",
    )

    val conditionalAdditionalConditionData = AdditionalConditionData(
      additionalCondition = anAdditionalCondition(id = 2, licence = aLicenceEntity),
      dataField = "conditionalName",
    )

    val anAdditionalConditionEntity = AdditionalCondition(
      id = 1,
      conditionVersion = "2.1",
      licence = aLicenceEntity,
      conditionCode = "code",
      conditionCategory = "category",
      conditionSequence = 4,
      conditionText = "text",
      additionalConditionData = mutableListOf(someAdditionalConditionData),
      additionalConditionUploadSummary = mutableListOf(),
      conditionType = "AP",
    )

    val aMappedPolicy = AllAdditionalConditions(
      mapOf(
        "2.1" to mapOf(
          policyApCondition.code to policyApCondition,
          policyApCondition.copy(code = "code2").code to policyApCondition.copy(code = "code2"),
        ),
      ),
    )

    val aMappedPolicyWithoutInputs = AllAdditionalConditions(
      mapOf(
        "2.1" to mapOf(
          policyConditionWithoutInput.code to policyConditionWithoutInput,
        ),
      ),
    )

    val aMappedPolicyWithMultipleInputs = AllAdditionalConditions(
      mapOf(
        "2.1" to mapOf(
          aPolicyConditionWithMultipleInputs.code to aPolicyConditionWithMultipleInputs,
        ),
      ),
    )

    val aMappedPolicyWithMultipleVersions = AllAdditionalConditions(
      mapOf(
        "2.0" to mapOf(
          policyApCondition.copy(code = "code2").code to policyApCondition.copy(code = "code2"),
        ),
        "2.1" to mapOf(
          policyApCondition.code to policyApCondition,
          policyConditionWithoutInput.copy(code = "code2").code to policyConditionWithoutInput.copy(code = "code2"),
        ),
      ),
    )

    val aMappedPolicyWithConditionalInputs = AllAdditionalConditions(
      mapOf(
        "2.1" to mapOf(
          aPolicyConditionWithConditionalInputs.code to aPolicyConditionWithConditionalInputs,
        ),
      ),
    )
  }
}
