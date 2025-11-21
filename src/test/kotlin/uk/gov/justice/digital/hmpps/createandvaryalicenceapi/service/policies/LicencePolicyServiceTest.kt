package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aModelLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.someOldModelAdditionalConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS
import java.time.LocalDate

class LicencePolicyServiceTest {
  private val progressionModelPolicyStartDate = LocalDate.now()
  private val licencePolicyService =
    LicencePolicyService(progressionModelPolicyStartDate = progressionModelPolicyStartDate)

  @Test
  fun `Policy version 3 is returned if licence start date is not provided`() {
    val policy = licencePolicyService.currentPolicy(null)
    assertThat(policy.version).isEqualTo("3.0")
  }

  @Test
  fun `Policy version 3 is returned if licence start date is before progress model policy start date`() {
    val policy = licencePolicyService.currentPolicy(progressionModelPolicyStartDate.minusDays(1))
    assertThat(policy.version).isEqualTo("3.0")
  }

  @Test
  fun `Policy version 3 is returned if progress model policy start date is null`() {
    val licencePolicyServiceNullStartDate = LicencePolicyService(progressionModelPolicyStartDate = null)

    val policy = licencePolicyServiceNullStartDate.currentPolicy(LocalDate.now())
    assertThat(policy.version).isEqualTo("3.0")
  }

  @Test
  fun `Policy version 4 is returned if licence start date is on or after progress model policy start date`() {
    assertThat(licencePolicyService.currentPolicy(progressionModelPolicyStartDate).version).isEqualTo("4.0")
    assertThat(licencePolicyService.currentPolicy(progressionModelPolicyStartDate.plusDays(1)).version).isEqualTo("4.0")
  }

  @Test
  fun `All versions are accessible`() {
    assertThat(licencePolicyService.allPolicies()).hasSize(5)
  }

  @Test
  fun `No policy versions have duplicate condition codes within the same policy`() {
    licencePolicyService.allPolicies().forEach { policy ->
      assertThat(policy.allAdditionalConditions().map { it.code }).doesNotHaveDuplicates()
      assertThat(policy.allStandardConditions().map { it.code }).doesNotHaveDuplicates()
    }
  }

  @Nested
  inner class `get config for condition` {
    @Test
    fun `can find config for condition`() {
      assertThat(
        licencePolicyService.getConfigForCondition("2.1", "ed607a91-fe3a-4816-8eb9-b447c945935c").code,
      ).isEqualTo("ed607a91-fe3a-4816-8eb9-b447c945935c")
    }

    @Test
    fun `can't find config for condition where code does not exist`() {
      val error = assertThrows<IllegalStateException> {
        licencePolicyService.getConfigForCondition("2.1", "does not exist")
      }

      assertThat(error.message).isEqualTo("Condition with code: 'does not exist' and version: '2.1' not found.")
    }

    @Test
    fun `can't find config for condition that does not exist on version`() {
      val error = assertThrows<IllegalStateException> {
        licencePolicyService.getConfigForCondition(
          // only exists on 1.0
          "2.1",
          "599bdcae-d545-461c-b1a9-02cb3d4ba268",
        )
      }

      assertThat(error.message).isEqualTo("Condition with code: '599bdcae-d545-461c-b1a9-02cb3d4ba268' and version: '2.1' not found.")
    }
  }

  @Nested
  inner class GetAllAdditionalConditions {
    @Test
    fun `builds object containing conditions for all policy versions`() {
      val allPolicyVersions = licencePolicyService.allPolicies().map { it.version }
      assertThat(licencePolicyService.getAllAdditionalConditions().mappedPolicy.keys.toList()).isEqualTo(
        allPolicyVersions,
      )
    }

    @Test
    fun `builds object containing all additional conditions from all policy versions`() {
      val allAdditionalConditions = licencePolicyService.allPolicies().map { it.allAdditionalConditions() }.flatten()
      assertThat(
        licencePolicyService.getAllAdditionalConditions().mappedPolicy.map { it.value.values }
          .flatten(),
      ).containsAll(allAdditionalConditions)
    }
  }

  @Nested
  inner class GetHardStopAdditionalConditions {
    @Test
    fun `get Hardstop conditions for PSS`() {
      val pssLicence = createCrdLicence().copy(typeCode = PSS)
      val conditions = licencePolicyService.getHardStopAdditionalConditions(pssLicence)
      assertThat(conditions).isEmpty()
    }

    @Test
    fun `get HardStop conditions for AP`() {
      val pssLicence = createCrdLicence().copy(id = 2L, typeCode = AP)
      val conditions = licencePolicyService.getHardStopAdditionalConditions(pssLicence)
      assertThat(conditions).hasSize(1)
      with(conditions.first()) {
        assertThat(licence.id).isEqualTo(2L)
        assertThat(conditionCode).isEqualTo(HARD_STOP_CONDITION.code)
        assertThat(conditionText).isEqualTo(HARD_STOP_CONDITION.text)
        assertThat(conditionSequence).isEqualTo(0L)
        assertThat(conditionType).isEqualTo("AP")
      }
    }

    @Test
    fun `get HardStop conditions for AP_PSS`() {
      val pssLicence = createCrdLicence().copy(id = 2L, typeCode = AP_PSS)
      val conditions = licencePolicyService.getHardStopAdditionalConditions(pssLicence)
      assertThat(conditions).hasSize(1)
      with(conditions.first()) {
        assertThat(licence.id).isEqualTo(2L)
        assertThat(conditionCode).isEqualTo(HARD_STOP_CONDITION.code)
        assertThat(conditionText).isEqualTo(HARD_STOP_CONDITION.text)
        assertThat(conditionSequence).isEqualTo(0L)
        assertThat(conditionType).isEqualTo("AP")
      }
    }
  }

  @Nested
  inner class CompareLicenceWithPolicy {
    @Test
    fun `returns an empty list if the previous licence is missing a version`() {
      val previousLicence = aModelLicence().copy(id = 1, version = null)
      val currentLicence = aModelLicence().copy(id = 2, statusCode = LicenceStatus.VARIATION_IN_PROGRESS)

      val policyChanges = licencePolicyService.compareLicenceWithPolicy(currentLicence, previousLicence, "2.1")

      assertThat(policyChanges).isEmpty()
    }

    @Test
    fun `returns an empty list if the previous licence policy version is equal to the current licence policy version`() {
      val previousLicence = aModelLicence().copy(id = 1, version = "2.0")
      val currentLicence =
        aModelLicence().copy(id = 2, statusCode = LicenceStatus.VARIATION_IN_PROGRESS, version = "2.0")

      val policyChanges = licencePolicyService.compareLicenceWithPolicy(currentLicence, previousLicence, "2.1")

      assertThat(policyChanges).isEmpty()
    }

    @Test
    fun `returns a list of policy changes`() {
      val previousLicence =
        aModelLicence().copy(id = 1, version = "2.0", additionalLicenceConditions = someOldModelAdditionalConditions())
      val currentLicence = aModelLicence().copy(
        id = 2,
        statusCode = LicenceStatus.VARIATION_IN_PROGRESS,
        version = "2.1",
        additionalLicenceConditions = someOldModelAdditionalConditions(),
      )

      val policyChanges = licencePolicyService.compareLicenceWithPolicy(currentLicence, previousLicence, "2.1")

      assertThat(policyChanges.size).isEqualTo(1)
    }
  }

  @Nested
  inner class `get conditions requiring electronic monitoring response` {
    @Test
    fun `returns empty list when no conditions require electronic monitoring response`() {
      val result = licencePolicyService.getConditionsRequiringElectronicMonitoringResponse(
        version = "2.1",
        conditionCodes = setOf("condition1", "condition3", "599bdcae-d545-461c-b1a9-02cb3d4ba268"),
      )
      assertThat(result).isEmpty()
    }
  }

  private companion object
}
