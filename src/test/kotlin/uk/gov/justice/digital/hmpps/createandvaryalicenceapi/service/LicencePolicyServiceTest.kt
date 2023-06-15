package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
}
