package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository

class NotifyAttentionNeededLicencesIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-attention-needed.sql",
  )
  fun `Run notify attention needed licences job`() {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()

    webTestClient.post()
      .uri("/run-notify-attention-needed-licences-job")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  private companion object {
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val govUkMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonerSearchMockServer.start()
      govUkMockServer.start()
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
