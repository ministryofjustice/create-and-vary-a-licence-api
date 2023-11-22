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
        - Edits of approved licences (pre-release) or variations of licence (post-release) create new licence records within the database. 
        Previously-approved and pre-variation licence records can still be found if they have not been made INACTIVE. If historic INACTIVE 
        licence information is required, please contact the CVL team.
        - In flight licences are those that have a non-INACTIVE status code. This represents licences that are either ACTIVE or 
        have the potential to become ACTIVE, and includes the following status codes:
          - [ACTIVE, IN_PROGRESS, SUBMITTED, APPROVED, VARIATION_IN_PROGRESS, VARIATION_SUBMITTED, VARIATION_APPROVED & VARIATION_REJECTED]
        - Condition data is different dependent on the type of condition:
          - Standard conditions have static text
          - Additional conditions may be static text or have a combination of static and inputted text
          - Bespoke conditions are purely inputted text
        - The list of status codes currently used by CVL is subject to change as new features may require additional codes to be added.
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
