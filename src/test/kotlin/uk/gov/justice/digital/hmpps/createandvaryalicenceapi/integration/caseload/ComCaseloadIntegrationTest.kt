package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TeamCaseloadRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.Charsets.UTF_8

private const val DELIUS_STAFF_IDENTIFIER = 3492L
private const val GET_STAFF_CREATE_CASELOAD = "/caseload/com/staff/$DELIUS_STAFF_IDENTIFIER/create-case-load"
private const val GET_TEAM_CREATE_CASELOAD = "/caseload/com/team/create-case-load"
private const val GET_STAFF_VARY_CASELOAD = "/caseload/com/staff/$DELIUS_STAFF_IDENTIFIER/vary-case-load"
private const val GET_TEAM_VARY_CASELOAD = "/caseload/com/team/vary-case-load"

class ComCaseloadIntegrationTest : IntegrationTestBase() {

  fun readFile(filename: String): String = this.javaClass.getResourceAsStream("/test_data/integration/caseload/$filename.json")!!.bufferedReader(UTF_8)
    .readText()

  @Nested
  inner class GetStaffCreateCaseload {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.get()
        .uri(GET_STAFF_CREATE_CASELOAD)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
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
        .uri(GET_STAFF_CREATE_CASELOAD)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun `Successfully retrieve staff create caseload`() {
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)
      deliusMockServer.stubGetProbationCases()
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        readFile("staff-create-case-load-prisoners").replace(
          "\$releaseDate",
          releaseDate,
        ),
      )
      prisonApiMockServer.stubGetCourtOutcomes()

      val caseload = webTestClient.get()
        .uri(GET_STAFF_CREATE_CASELOAD)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(3)
      with(caseload.first()) {
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(prisonerNumber).isEqualTo("AB1234E")
      }
    }
  }

  @Nested
  inner class GetTeamCreateCaseload {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(GET_TEAM_CREATE_CASELOAD)
        .bodyValue(TeamCaseloadRequest(listOf("team a", "team b"), listOf("team c")))
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
        .exchange()
        .expectStatus().isForbidden
        .expectStatus().isEqualTo(FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.post()
        .uri(GET_TEAM_CREATE_CASELOAD)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun `Successfully retrieve team create caseload`() {
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffendersByTeam("teamC")
      deliusMockServer.stubGetProbationCases(readFile("com-case-load-offenders"))
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        readFile("team-create-case-load-prisoners").replace(
          "\$releaseDate",
          releaseDate,
        ),
      )
      prisonApiMockServer.stubGetCourtOutcomes()

      val caseload = webTestClient.post()
        .uri(GET_TEAM_CREATE_CASELOAD)
        .bodyValue(TeamCaseloadRequest(listOf("teamA", "teamB"), listOf("teamC")))
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(2)
      with(caseload.first()) {
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(prisonerNumber).isEqualTo("AB1234E")
      }
    }
  }

  @Nested
  inner class GetStaffVaryCaseload {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.get()
        .uri(GET_STAFF_VARY_CASELOAD)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
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
        .uri(GET_STAFF_VARY_CASELOAD)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence-for-staff-vary-caseload.sql",
  )
  fun `Successfully retrieve staff vary caseload`() {
    deliusMockServer.stubGetStaffDetailsByUsername()
    deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)

    val caseload = webTestClient.get()
      .uri(GET_STAFF_VARY_CASELOAD)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isEqualTo(OK.value())
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody(typeReference<List<ComCase>>())
      .returnResult().responseBody!!

    assertThat(caseload).hasSize(1)
    with(caseload.first()) {
      assertThat(crnNumber).isEqualTo("X12348")
      assertThat(prisonerNumber).isEqualTo("AB1234E")
    }
  }

  @Nested
  inner class GetTeamVaryCaseload {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(GET_TEAM_VARY_CASELOAD)
        .bodyValue(TeamCaseloadRequest(listOf("team a", "team b"), listOf("team c")))
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
        .exchange()
        .expectStatus().isForbidden
        .expectStatus().isEqualTo(FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.post()
        .uri(GET_TEAM_VARY_CASELOAD)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    @Sql(
      "classpath:test_data/seed-variation-licence-for-team-vary-caseload.sql",
    )
    fun `Successfully retrieve team vary caseload`() {
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffendersByTeam("teamC")

      val caseload = webTestClient.post()
        .uri(GET_TEAM_VARY_CASELOAD)
        .bodyValue(TeamCaseloadRequest(listOf("teamA", "teamB"), listOf("teamC")))
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(1)
      with(caseload.first()) {
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(prisonerNumber).isEqualTo("AB1234E")
      }
    }
  }

  private companion object {
    val prisonerSearchApiMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val govUkMockServer = GovUkMockServer()
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonerSearchApiMockServer.start()
      deliusMockServer.start()
      govUkMockServer.start()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchApiMockServer.stop()
      deliusMockServer.stop()
      govUkMockServer.stop()
      prisonApiMockServer.stop()
    }
  }
}
