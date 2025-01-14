package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.HARD_STOP_CONDITION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP_PSS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.PSS

class SarLicencePolicyServiceTest() {

  private val licencePolicyService = LicencePolicyService()

  @Test
  fun `Current policy version is returned`() {
    val policy = licencePolicyService.currentPolicy()
    assertThat(policy.version).isEqualTo("2.1")
  }

  @Test
  fun `All versions are accessible`() {
    assertThat(licencePolicyService.allPolicies()).hasSize(3)
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
  inner class `getAllSarAdditionalConditions` {
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
  inner class `getHardStopSarAdditionalConditions` {
    @Test
    fun `get Hardstop conditions for PSS`() {
      val pssLicence = TestData.createCrdLicence().copy(typeCode = PSS)
      val conditions = licencePolicyService.getHardStopAdditionalConditions(pssLicence)
      assertThat(conditions).isEmpty()
    }

    @Test
    fun `get HardStop conditions for AP`() {
      val pssLicence = TestData.createCrdLicence().copy(id = 2L, typeCode = AP)
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
      val pssLicence = TestData.createCrdLicence().copy(id = 2L, typeCode = AP_PSS)
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
}
