package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

@Configuration
class JacksonConfig {

  @Bean
  fun objectMapper(): ObjectMapper = JsonMapper.builder()
    .findAndAddModules() // auto-detect JavaTimeModule, ParameterNamesModule, etc.
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .addModule(
      KotlinModule.Builder()
        .configure(KotlinFeature.StrictNullChecks, false)
        .build(),
    )
    .build()

  @Bean
  fun customizer(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder: JsonMapper.Builder? ->
    builder!!
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .addModule(
        KotlinModule.Builder()
          .configure(KotlinFeature.StrictNullChecks, false)
          .build(),
      )
  }
}
