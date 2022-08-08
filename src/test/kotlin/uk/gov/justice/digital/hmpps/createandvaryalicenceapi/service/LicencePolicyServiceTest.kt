package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class LicencePolicyServiceTest() {

  private val licencePolicyService = LicencePolicyService(
    listOf(
      ClassPathResource("/test_data/policy_conditions/policyV1.json").file,
      ClassPathResource("/test_data/policy_conditions/policyV2.json").file
    ).map { policy -> jacksonObjectMapper().readValue(policy) }
  )

  @Test
  fun `Current policy version is returned`() {
    val policy = licencePolicyService.currentPolicy()
    assertThat(policy.version).isEqualTo("2.0")
  }

  @Test
  fun `Both versions are accessible`() {
    assertThat(licencePolicyService.policyByVersion("1.0").version).isEqualTo("1.0")
    assertThat(licencePolicyService.policyByVersion("2.0").version).isEqualTo("2.0")
  }

  @Test
  fun `Compare returns the correct differences for Standard Conditions`() {
    val policy1 = licencePolicyService.policyByVersion("1.0")
    val policy2 = licencePolicyService.policyByVersion("2.0")

    val diff = licencePolicyService.compare(policy1, policy2)

    assertThat(diff.standardConditions.Ap.addedConditions.count()).isEqualTo(1)
    assertThat(diff.standardConditions.Ap.amendedConditions.count()).isEqualTo(0)
    assertThat(diff.standardConditions.Ap.removedConditions.count()).isEqualTo(0)

    val newApCondition = diff.standardConditions.Ap.addedConditions.first()
    assertThat(newApCondition.code).isEqualTo("3b19fdb0-4ca3-4615-9fdd-aaaaaaaaaaaa")
    assertThat(newApCondition.text).isEqualTo("A new AP standard condition.")

    assertThat(diff.standardConditions.Pss.addedConditions.count()).isEqualTo(0)
    assertThat(diff.standardConditions.Pss.amendedConditions.count()).isEqualTo(0)
    assertThat(diff.standardConditions.Pss.removedConditions.count()).isEqualTo(1)

    val removedPssCondition = diff.standardConditions.Pss.removedConditions.first()
    assertThat(removedPssCondition.code).isEqualTo("b950407d-2270-45b8-9666-3ad58a17d0be")
  }

  @Test
  fun `Compare returns the correct differences for Additional Conditions`() {
    val policy1 = licencePolicyService.policyByVersion("1.0")
    val policy2 = licencePolicyService.policyByVersion("2.0")

    val diff = licencePolicyService.compare(policy1, policy2)

    assertThat(diff.additionalConditions.Ap.addedConditions.count()).isEqualTo(0)
    assertThat(diff.additionalConditions.Ap.amendedConditions.count()).isEqualTo(1)
    assertThat(diff.additionalConditions.Ap.removedConditions.count()).isEqualTo(0)

    val removedApCondition = diff.additionalConditions.Ap.amendedConditions.first()
    assertThat(removedApCondition.first.code).isEqualTo("5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd")
    assertThat(removedApCondition.second.code).isEqualTo("5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd")

    assertThat(removedApCondition.first.category).isEqualTo("Residence at a specific place")
    assertThat(removedApCondition.second.category).isEqualTo("Residence at a NONE specific place")

    assertThat(diff.additionalConditions.Pss.addedConditions.count()).isEqualTo(1)
    assertThat(diff.additionalConditions.Pss.amendedConditions.count()).isEqualTo(0)
    assertThat(diff.additionalConditions.Pss.removedConditions.count()).isEqualTo(0)

    val addedPssCondition = diff.additionalConditions.Pss.addedConditions.first()

    assertThat(addedPssCondition.code).isEqualTo("fda24aa9-a2b0-4d49-9c87-23b0a7be4013")
  }
}
