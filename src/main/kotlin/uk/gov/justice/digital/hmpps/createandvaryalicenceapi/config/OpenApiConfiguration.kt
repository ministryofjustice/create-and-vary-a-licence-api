package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun internalGroupedConfig() = GroupedOpenApi.builder()
    .group("internal")
    .packagesToScan("uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.internal")
    .build()

  @Bean
  fun externalGroupedConfig() = GroupedOpenApi.builder()
    .group("external")
    .packagesToScan("uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.external")
    .build()

  @Bean
  fun customOpenAPI(buildProperties: BuildProperties): OpenAPI? = OpenAPI()
    .info(
      Info()
        .title("Create and Vary a Licence API")
        .version(version)
        .description("API for access to licence data")
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk"),
        ),
    )
}
