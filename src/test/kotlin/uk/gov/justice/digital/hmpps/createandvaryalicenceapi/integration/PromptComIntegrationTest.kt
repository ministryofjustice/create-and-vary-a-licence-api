package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.Com
import java.time.Duration

class PromptComIntegrationTest : IntegrationTestBase() {
  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-prompting.sql",
  )
  fun `Retrieve list of cases to prompt COMs about`() {
    val coms = webTestClient.get()
      .uri("/jobs/prompt-com")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(Com::class.java)
      .returnResult().responseBody
    assertThat(coms?.size).isEqualTo(1)
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()
    val prisonSearchServer = PrisonerSearchMockServer()
    val probationMockServer = ProbationSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val prisonMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
      prisonSearchServer.start()
      prisonSearchServer.stubSearchPrisonersByReleaseDate(0, inHardStop = false)
      probationMockServer.start()
      probationMockServer.stubSearchForPersonByNomsNumberForGetApprovalCaseload()
      deliusMockServer.start()
      deliusMockServer.stubGetOffenderManagers()
      prisonMockServer.start()
      prisonMockServer.stubGetCourtOutcomes()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      prisonSearchServer.stop()
      probationMockServer.stop()
      deliusMockServer.stop()
      prisonMockServer.stop()
    }
  }
}
