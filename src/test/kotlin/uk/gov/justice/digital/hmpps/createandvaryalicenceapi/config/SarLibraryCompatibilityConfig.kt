package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

/*
  A shadowing trick :-)

  This bean replaces the one in the HMPPS SAR library.
  The library version currently has binary incompatibilities
  with Spring Boot 4 / Mockito 6 method signatures.
 */
@TestConfiguration
open class SarLibraryCompatibilityConfig {

  @Bean
  @Primary
  open fun sarIntegrationTestHelper(
    jwtAuthHelper: JwtAuthorisationHelper,
    @Value("\${hmpps.sar.tests.expected-api-response.path:}") expectedApiResponsePath: String,
    @Value("\${hmpps.sar.tests.expected-render-result.path:}") expectedRenderResultPath: String,
    @Value("\${hmpps.sar.tests.attachments-expected:false}") attachmentsExpected: Boolean,
    @Value("\${hmpps.sar.tests.expected-flyway-schema-version:0}") expectedFlywaySchemaVersion: String,
    @Value("\${hmpps.sar.tests.expected-jpa-entity-schema.path:}") expectedJpaEntitySchemaPath: String,
  ): SarIntegrationTestHelper = SarIntegrationTestHelper(
    jwtAuthHelper,
    expectedApiResponsePath,
    expectedRenderResultPath,
    attachmentsExpected,
    expectedFlywaySchemaVersion,
    expectedJpaEntitySchemaPath,
  )
}
