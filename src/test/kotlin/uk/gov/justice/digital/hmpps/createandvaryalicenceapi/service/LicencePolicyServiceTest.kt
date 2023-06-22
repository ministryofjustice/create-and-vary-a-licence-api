package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class LicencePolicyServiceTest() {

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
        licencePolicyService.getConfigForCondition(
          condition.copy(
            conditionVersion = "2.1",
            conditionCode = "ed607a91-fe3a-4816-8eb9-b447c945935c",
          ),
        ).code,
      ).isEqualTo("ed607a91-fe3a-4816-8eb9-b447c945935c")
    }

    @Test
    fun `can't find config for condition where code does not exist`() {
      val error = assertThrows<IllegalStateException> {
        licencePolicyService.getConfigForCondition(
          condition.copy(
            conditionVersion = "2.1",
            conditionCode = "does not exist",
          ),
        )
      }

      assertThat(error.message).isEqualTo("Condition with code: 'does not exist' and version: '2.1' not found.")
    }

    @Test
    fun `can't find config for condition that does not exist on version`() {
      val error = assertThrows<IllegalStateException> {
        licencePolicyService.getConfigForCondition(
          condition.copy(
            conditionVersion = "2.1", // only exists on 1.0
            conditionCode = "599bdcae-d545-461c-b1a9-02cb3d4ba268",
          ),
        )
      }

      assertThat(error.message).isEqualTo("Condition with code: '599bdcae-d545-461c-b1a9-02cb3d4ba268' and version: '2.1' not found.")
    }

    val licence = Licence(
      id = 2L,
      crn = "exampleCrn",
      forename = "Robin",
      surname = "Smith",
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.ACTIVE,
      version = "1.0",
      probationAreaCode = "N01",
      probationAreaDescription = "N01 Region",
      probationPduCode = "PDU1",
      probationPduDescription = "PDU1 Pdu",
      probationLauCode = "LAU1",
      probationLauDescription = "LAU1 Lau",
      probationTeamCode = "TEAM1",
      probationTeamDescription = "TEAM1 probation team",
    )

    val condition = AdditionalCondition(
      id = 1,
      conditionCode = "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
      conditionCategory = "Residence at a specific place",
      conditionSequence = 0,
      conditionText = "You must reside within the [INSERT REGION] while of no fixed abode, unless otherwise approved by your supervising officer.",
      additionalConditionData = emptyList(),
      additionalConditionUploadSummary = emptyList(),
      conditionVersion = "1.0",
      licence = licence,
    )
  }
}
