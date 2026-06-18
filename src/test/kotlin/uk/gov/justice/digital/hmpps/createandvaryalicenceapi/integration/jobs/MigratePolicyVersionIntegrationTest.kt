package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.MigrateStandardConditionsService

class MigratePolicyVersionIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrateStandardConditionsService: MigrateStandardConditionsService

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Migrate standard conditions`() {
    webTestClient.post()
      .uri("/jobs/migrate-standard-conditions?policyVersion=4.0")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNoContent

    getLicence(1).run {
      assertThat(standardLicenceConditions).isNotEmpty()
      assertThat(standardLicenceConditions).hasSize(10)
    }
  }

  private fun getLicence(id: Long) = webTestClient.get()
    .uri("/licence/id/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<Licence>()
    .returnResult().responseBody
}
