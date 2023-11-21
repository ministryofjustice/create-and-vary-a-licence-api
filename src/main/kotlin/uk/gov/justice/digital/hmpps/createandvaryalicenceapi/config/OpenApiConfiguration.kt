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

  private val publicApiDescription: String =
    """
      API for access to licence data.
      
      Please note:
      - Variations and edits to licences are classed as new licences
      - In flight licences are licences with the following status:
        - [ACTIVE, IN_PROGRESS, SUBMITTED, APPROVED, VARIATION_IN_PROGRESS, VARIATION_SUBMITTED, VARIATION_APPROVED &
      VARIATION_REJECTED]
      - Condition data is different dependent on the type of condition: 
          - Standard conditions have static text
          - Additional conditions have a combination of static and inputted text
          - Bespoke conditions are purely inputted text
          
    """.trimIndent()

  @Bean
  fun privateGroupedConfig(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("private")
    .packagesToScan("uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.privateApi")
    .build()

  @Bean
  fun publicGroupedConfig(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("public")
    .addOpenApiCustomizer {
        a ->
      a.info(
        Info()
          .title("Create and Vary a Licence Public API")
          .description(publicApiDescription)
          .version(version)
          .contact(
            Contact()
              .name("HMPPS Digital Studio")
              .email("feedback@digital.justice.gov.uk"),
          ),
      )
    }
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
