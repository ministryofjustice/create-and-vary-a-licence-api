package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom.PromptComNotification
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
  fun `Trying to get case list with no authorisation`() {
    webTestClient.get()
      .uri("/coms-to-prompt")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-prompting.sql",
  )
  fun `Trying to get case list with no roles`() {
    webTestClient.get()
      .uri("/coms-to-prompt")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-prompting.sql",
  )
  fun `Retrieve list of cases to prompt COMs about`() {
    val coms = webTestClient.get()
      .uri("/coms-to-prompt")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBodyList(PromptComNotification::class.java)
      .returnResult().responseBody
    assertThat(coms?.size).isEqualTo(1)
    assertThat(coms?.get(0)?.initialPromptCases?.size).isEqualTo(2)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-prompting.sql",
  )
  fun `Run job requires no roles`() {
    webTestClient.post()
      .uri("/jobs/prompt-licence-creation")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNoContent

    await untilAsserted { verify(telemetryClient).trackEvent("PromptComJob", mapOf("cases" to "1"), null) }
  }

  @BeforeEach
  fun beforeEach() {
    prisonMockServer.stubGetCourtOutcomes()
    prisonMockServer.stubGetSentenceAndRecallTypes(124)
    deliusMockServer.stubGetManagersForPromptComJob()
    prisonSearchServer.stubSearchPrisonersByReleaseDate(0, inHardStop = false, includeRecall = true)
  }

  private companion object {
    @RegisterExtension
    val prisonSearchServer = PrisonerSearchMockServer()

    @RegisterExtension
    val deliusMockServer = DeliusMockServer()

    @RegisterExtension
    val prisonMockServer = PrisonApiMockServer()
  }
}
