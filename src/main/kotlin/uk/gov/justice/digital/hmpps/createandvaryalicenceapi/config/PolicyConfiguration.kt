package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicencePolicyService

@Configuration
class PolicyConfiguration(@Value("classpath:policy_conditions/*.json") private val conditions: Array<Resource>) {
  @Bean
  fun policies(): LicencePolicyService {
    return LicencePolicyService(conditions.map { policy -> jacksonObjectMapper().readValue(policy.file) })
  }
}
