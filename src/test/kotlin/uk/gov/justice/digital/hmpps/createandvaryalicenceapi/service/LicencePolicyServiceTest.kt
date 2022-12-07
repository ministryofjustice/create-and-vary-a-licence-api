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
}
