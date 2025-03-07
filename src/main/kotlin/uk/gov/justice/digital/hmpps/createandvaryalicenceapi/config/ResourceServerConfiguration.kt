package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
@EnableCaching
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
class ResourceServerConfiguration {

  @Bean
  fun securityFilterChain(
    http: HttpSecurity,
    resourceServerCustomizer: ResourceServerConfigurationCustomizer,
  ): SecurityFilterChain = HmppsResourceServerConfiguration().hmppsSecurityFilterChain(http, resourceServerCustomizer)

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/webjars/**",
        "/favicon.ico",
        "/health/**",
        "/info",
        "/h2-console/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-resources",
        "/swagger-resources/configuration/ui",
        "/swagger-resources/configuration/security",
        "/jobs/**",
      )
    }
  }
}
