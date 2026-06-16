package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.migration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.repository.MigrationRepository

class MigrationServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: MigrationRepository

  @Test
  @Sql("classpath:test_data/seed-an-hdc-migration.sql")
  fun `isAMigratedLicence returns true for migrated licences`() {
    Assertions.assertThat(repository.isAMigratedLicence(1)).isTrue()
  }

  @Test
  @Sql("classpath:test_data/seed-an-hdc-migration.sql")
  fun `isAMigratedLicence returns false for non-migrated licences`() {
    Assertions.assertThat(repository.isAMigratedLicence(2)).isFalse()
  }
}
