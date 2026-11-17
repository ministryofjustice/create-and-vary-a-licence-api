package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType

class HdcApplicationIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @ParameterizedTest
  @EnumSource(AccommodationType::class)
  @Sql("classpath:test_data/seed-hdc-licence-id-1.sql")
  fun `database accepts accommodation types`(type: AccommodationType) {
    jdbcTemplate.update("INSERT INTO hdc_application (licence_id, proposed_accommodation_type) VALUES (1, ?)", type.name)

    assertThat(testRepository.findAllHdcPostponements().single().proposedAccommodationType).isEqualTo(type)
  }

  @Test
  @Sql("classpath:test_data/seed-hdc-licence-id-1.sql")
  fun `database rejects invalid accommodation type`() {
    val exception = assertThrows<DataIntegrityViolationException> {
      jdbcTemplate.update("INSERT INTO hdc_application (licence_id, proposed_accommodation_type) VALUES (1, ?)", "INVALID")
    }

    assertThat(exception.message).contains("chk_hdc_application_proposed_accommodation_type_valid")
  }
}
