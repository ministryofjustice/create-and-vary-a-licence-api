package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicencePolicyDto

@Configuration
class PolicyConfiguration(@Value("classpath:policy_conditions/*.json") private val conditions: Array<Resource>) {

  final var policies: List<LicencePolicyDto> =
    conditions.map { policy -> jacksonObjectMapper().readValue(policy.file) }
  final var currentPolicy = policies.maxBy { it.version }

  @Bean
  @Qualifier("policyConditions")
  fun policies(): List<LicencePolicyDto> {
    return policies
  }

  @Bean
  @Qualifier("currentPolicy")
  fun currentPolicy(): LicencePolicyDto {
    return currentPolicy
  }
}
