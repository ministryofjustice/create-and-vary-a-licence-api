package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.json.JsonCompareMode.STRICT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import java.nio.charset.StandardCharsets.UTF_8

private const val GET_CASES = "/cvl-report/upcoming-releases-with-monitoring"

class UpcomingReleasesWithMonitoringConditionsIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun setup() {
    webTestClient = webTestClient.mutate().build()
  }

  @Sql(
    "classpath:test_data/upcoming_releases_with_em_conditions.sql",
  )
  @Test
  fun `Successfully retrieve some cases`() {
    // Given

    // When
    val result = webTestClient.get()
      .uri(GET_CASES)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .accept(APPLICATION_JSON)
      .exchange()

    // Then
    result.expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
      .json(serializedContent("upcoming_releases_with_em_conditions"), STRICT)
  }

  private fun serializedContent(name: String) = this.javaClass.getResourceAsStream("/test_data/reports/$name.json")!!.bufferedReader(
    UTF_8,
  ).readText()

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.get()
      .uri(GET_CASES)
      .accept(APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG_ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectStatus().isEqualTo(FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.get()
      .uri(GET_CASES)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(UNAUTHORIZED.value())
  }

  companion object {
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonerSearchMockServer.start()
      deliusMockServer.start()
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      deliusMockServer.stop()
      prisonApiMockServer.stop()
    }
  }
}
