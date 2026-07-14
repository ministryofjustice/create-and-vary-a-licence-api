package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.MigrateStandardConditionsService
import java.time.Duration

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

    await.atMost(Duration.ofSeconds(30)) untilCallTo {
      val licence = testRepository.findLicence(1L)
      licence.standardConditions.size
    } matches { size -> size == 8 }
  }
}
