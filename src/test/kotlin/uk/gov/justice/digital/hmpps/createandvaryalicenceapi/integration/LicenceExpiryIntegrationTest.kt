package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class LicenceExpiryIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-expiry.sql",
  )
  fun `Run licence expiry job`() {
    webTestClient.post()
      .uri("/run-expire-licences-job")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val inactiveLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.INACTIVE)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(inactiveLicences?.size).isEqualTo(4)
    assertThat(inactiveLicences)
      .extracting<Tuple> {
        Tuple.tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        Tuple.tuple(2L, LicenceStatus.INACTIVE),
        Tuple.tuple(5L, LicenceStatus.INACTIVE),
        Tuple.tuple(6L, LicenceStatus.INACTIVE),
        Tuple.tuple(7L, LicenceStatus.INACTIVE),
      )

    val remainingLicences = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = LicenceStatus.IN_FLIGHT_LICENCES))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(remainingLicences?.size).isEqualTo(3)
    assertThat(remainingLicences)
      .extracting<Tuple> {
        Tuple.tuple(it.licenceId, it.licenceStatus)
      }
      .contains(
        Tuple.tuple(1L, LicenceStatus.APPROVED),
        Tuple.tuple(3L, LicenceStatus.ACTIVE),
        Tuple.tuple(4L, LicenceStatus.IN_PROGRESS),
      )
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
