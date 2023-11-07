package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.publicApi

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.ConditionTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicyConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V1_0
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_0
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.POLICY_V2_1

class PublicLicencePolicyServiceTest {
  private val licencePolicyService = mock<LicencePolicyService>()

  private val service = PublicLicencePolicyService(licencePolicyService)

  @Test
  fun `service returns a policy by version number`() {
    whenever(licencePolicyService.allPolicies()).thenReturn(
      listOf(POLICY_V1_0, POLICY_V2_0, POLICY_V2_1),
    )

    val policy = service.getLicencePolicyByVersionNumber("2.1")

    assertThat(policy).isExactlyInstanceOf(LicencePolicy::class.java)

    assertThat(policy.version).isEqualTo("2.1")

    assertThat(policy.conditions).isExactlyInstanceOf(ConditionTypes::class.java)

    assertThat(policy.conditions.apConditions).isExactlyInstanceOf(LicencePolicyConditions::class.java)

    assertThat(policy.conditions.pssConditions).isExactlyInstanceOf(LicencePolicyConditions::class.java)

    val anApStandardCondition = policy.conditions.apConditions.standard.first()

    assertThat(anApStandardCondition)
      .extracting {
        Tuple.tuple(it.code, it.text)
      }
      .isEqualTo(
        Tuple.tuple(
          "9ce9d594-e346-4785-9642-c87e764bee37",
          "Be of good behaviour and not behave in a way which undermines the purpose of the licence period.",
        ),
      )

    val anApAdditionalCondition = policy.conditions.apConditions.additional.first()

    assertThat(anApAdditionalCondition)
      .extracting {
        Tuple.tuple(it.code, it.text, it.category, it.categoryShort, it.requiresInput)
      }
      .isEqualTo(
        Tuple.tuple(
          "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
          "You must reside overnight within [REGION] probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
          "Residence at a specific place",
          null,
          true,
        ),
      )

    val aPssStandardCondition = policy.conditions.pssConditions.standard.first()

    assertThat(aPssStandardCondition)
      .extracting {
        Tuple.tuple(it.code, it.text)
      }
      .isEqualTo(
        Tuple.tuple(
          "b3cd4a30-11fd-4715-9ebb-ed89f5386e1f",
          "Be of good behaviour and not behave in a way that undermines the rehabilitative purpose of the supervision period.",
        ),
      )

    val aPssAdditionalCondition = policy.conditions.pssConditions.additional.first()

    assertThat(aPssAdditionalCondition)
      .extracting {
        Tuple.tuple(it.code, it.text, it.category, it.categoryShort, it.requiresInput)
      }
      .isEqualTo(
        Tuple.tuple(
          "62c83b80-2223-4562-a195-0670f4072088",
          "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
          "Drug appointment",
          null,
          true,
        ),
      )
  }

  @Test
  fun `service throws an exception when policy version does not exist`() {
    whenever(licencePolicyService.allPolicies()).thenReturn(
      listOf(POLICY_V1_0, POLICY_V2_0, POLICY_V2_1),
    )

    val exception = assertThrows<EntityNotFoundException> {
      service.getLicencePolicyByVersionNumber("0")
    }

    assertThat(exception)
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Policy version 0 not found")
  }

  @Test
  fun `given policies when get latest policy then return latest policy`() {
    whenever(licencePolicyService.currentPolicy()).thenReturn(POLICY_V2_1)

    val policy = service.getLatestLicencePolicy()

    assertThat(policy.version).isEqualTo(POLICY_V2_1.version)
  }
}
