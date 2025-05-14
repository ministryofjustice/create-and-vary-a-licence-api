package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository

class RemoveExpiredConditionsIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-4.sql",
  )
  fun `Update sentence dates`() {
    webTestClient.post()
      .uri("/jobs/remove-expired-conditions")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    // In PSS period
    getLicence(3).run {
      // AP conditions should be deleted i.e. empty if licence is in PSS period
      assertThat(additionalLicenceConditions).isEmpty()
      // Bespoke conditions should be deleted
      assertThat(bespokeConditions).isEmpty()

      // PSS conditions should not be deleted
      assertThat(additionalPssConditions).hasSize(2)
      assertThat(additionalPssConditions.first().id).isEqualTo(2)

      assertThat(standardLicenceConditions).isEmpty()
      assertThat(standardPssConditions).hasSize(1)
    }

    // In PSS period
    getLicence(4).run {
      assertThat(additionalLicenceConditions).hasSize(0)
      assertThat(bespokeConditions).hasSize(0)

      assertThat(additionalPssConditions).hasSize(1)
      assertThat(additionalPssConditions.first().id).isEqualTo(7)

      assertThat(standardLicenceConditions).isEmpty()
      assertThat(standardPssConditions).hasSize(2)
    }

    // Before PSS period
    getLicence(5).run {
      assertThat(additionalLicenceConditions).hasSize(1)
      assertThat(additionalLicenceConditions.first().id).isEqualTo(8)

      assertThat(bespokeConditions).hasSize(1)
      assertThat(bespokeConditions.first().id).isEqualTo(6)

      assertThat(additionalPssConditions).hasSize(1)
      assertThat(additionalPssConditions.first().id).isEqualTo(9)

      assertThat(standardLicenceConditions).hasSize(2)
      assertThat(standardPssConditions).hasSize(1)
    }

    // In PSS period but ACTIVE
    getLicence(6).run {
      assertThat(additionalLicenceConditions).hasSize(1)
      assertThat(bespokeConditions).hasSize(1)

      assertThat(additionalPssConditions).hasSize(1)
      assertThat(additionalPssConditions.first().id).isEqualTo(11)

      assertThat(standardLicenceConditions).hasSize(1)
      assertThat(standardPssConditions).hasSize(1)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-inprogress-variation.sql",
  )
  fun `Update sentence dates for HDC variation licences`() {
    webTestClient.post()
      .uri("/jobs/remove-expired-conditions")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    // In PSS period
    getLicence(3).run {
      // AP conditions should be deleted i.e. empty if licence is in PSS period
      assertThat(additionalLicenceConditions).isEmpty()
      // Bespoke conditions should be deleted
      assertThat(bespokeConditions).isEmpty()

      // PSS conditions should not be deleted
      assertThat(additionalPssConditions).hasSize(2)
      assertThat(additionalPssConditions.first().id).isEqualTo(2)

      assertThat(standardLicenceConditions).isEmpty()
      assertThat(standardPssConditions).hasSize(1)
    }

    // In PSS period
    getLicence(4).run {
      assertThat(additionalLicenceConditions).hasSize(0)
      assertThat(bespokeConditions).hasSize(0)

      assertThat(additionalPssConditions).hasSize(1)
      assertThat(additionalPssConditions.first().id).isEqualTo(7)

      assertThat(standardLicenceConditions).isEmpty()
      assertThat(standardPssConditions).hasSize(2)
    }

    // Before PSS period
    getLicence(5).run {
      assertThat(additionalLicenceConditions).hasSize(1)
      assertThat(additionalLicenceConditions.first().id).isEqualTo(8)

      assertThat(bespokeConditions).hasSize(1)
      assertThat(bespokeConditions.first().id).isEqualTo(6)

      assertThat(additionalPssConditions).hasSize(1)
      assertThat(additionalPssConditions.first().id).isEqualTo(9)

      assertThat(standardLicenceConditions).hasSize(2)
      assertThat(standardPssConditions).hasSize(1)
    }

    // In PSS period but ACTIVE
    getLicence(6).run {
      assertThat(additionalLicenceConditions).hasSize(1)
      assertThat(bespokeConditions).hasSize(1)

      assertThat(additionalPssConditions).hasSize(1)
      assertThat(additionalPssConditions.first().id).isEqualTo(11)

      assertThat(standardLicenceConditions).hasSize(1)
      assertThat(standardPssConditions).hasSize(1)
    }
  }

  private fun getLicence(id: Long) = webTestClient.get()
    .uri("/licence/id/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Licence::class.java)
    .returnResult().responseBody!!

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
