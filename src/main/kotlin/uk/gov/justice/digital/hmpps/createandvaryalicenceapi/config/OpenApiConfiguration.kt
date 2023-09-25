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
  fun privateGroupedConfig(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("private")
    .packagesToScan("uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi")
    .build()

  @Bean
  fun publicGroupedConfig(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("public")
    .packagesToScan("uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi")
    .build()

  @Bean
  fun customOpenAPI(): OpenAPI? = OpenAPI()
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
