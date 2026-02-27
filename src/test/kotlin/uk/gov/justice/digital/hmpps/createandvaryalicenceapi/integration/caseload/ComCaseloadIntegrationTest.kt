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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ComCreateCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TeamCaseloadRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.BookingSentenceAndRecallTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceAndRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
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
    fun `Successfully retrieve staff create caseload with no licences`() {
      val ftr14Ora =
        SentenceAndRecallType(
          "14FTR_ORA",
          RecallType("FIXED_TERM_RECALL_14", isStandardRecall = false, isFixedTermRecall = true),
        )
      val lrSopc21 =
        SentenceAndRecallType(
          "LR_SOPC21",
          RecallType("STANDARD_RECALL", isStandardRecall = true, isFixedTermRecall = false),
        )
      val accessResponse = """
      {
        "access": [
           {
            "crn": "X12348",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12351",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12352",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          },
          {
            "crn": "X12353",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
      """.trimIndent()

      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)
      deliusMockServer.stubGetCheckUserAccess(accessResponse)
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      val sled = LocalDate.now().plusDays(11).format(DateTimeFormatter.ISO_DATE)
      val tused = LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_DATE)
      stubSearchPrisonersByNomisId(releaseDate, sled, tused)
      prisonApiMockServer.stubGetCourtOutcomes()
      prisonApiMockServer.stubGetSentenceAndRecallTypes(
        listOf(
          BookingSentenceAndRecallTypes(6, listOf(ftr14Ora)),
          BookingSentenceAndRecallTypes(7, listOf(lrSopc21)),
        ),
      )

      val caseload = webTestClient.get()
        .uri(GET_STAFF_CREATE_CASELOAD)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCreateCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(4)
      assertThat(caseload.map { it.prisonerNumber }).containsExactlyInAnyOrder(
        "AB1234E",
        "AB1234H",
        "AB1234I",
        "AB1234J",
      )
      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12352")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }
      with(caseload[1]) {
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(prisonerNumber).isEqualTo("AB1234E")
      }
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licences-with-e_m_providers.sql",
    )
    fun `Successfully retrieve a caseload with licences`() {
      // Given
      val ftr14Ora =
        SentenceAndRecallType(
          "14FTR_ORA",
          RecallType("FIXED_TERM_RECALL_14", isStandardRecall = false, isFixedTermRecall = true),
        )
      val lrSopc21 =
        SentenceAndRecallType(
          "LR_SOPC21",
          RecallType("STANDARD_RECALL", isStandardRecall = true, isFixedTermRecall = false),
        )
      val accessResponse = """
      {
        "access": [
           {
            "crn": "X12348",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12351",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12352",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          },
          {
            "crn": "X12353",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
      """.trimIndent()
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)
      deliusMockServer.stubGetCheckUserAccess(accessResponse)
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      val sled = LocalDate.now().plusDays(11).format(DateTimeFormatter.ISO_DATE)
      val tused = LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_DATE)
      stubSearchPrisonersByNomisId(releaseDate, sled, tused)
      prisonApiMockServer.stubGetCourtOutcomes()
      prisonApiMockServer.stubGetSentenceAndRecallTypes(
        listOf(
          BookingSentenceAndRecallTypes(6, listOf(ftr14Ora)),
          BookingSentenceAndRecallTypes(7, listOf(lrSopc21)),
        ),
      )

      // When
      val result = webTestClient.get()
        .uri(GET_STAFF_CREATE_CASELOAD)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()

      // Then
      val caseload = result.expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCreateCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(4)

      assertThat(caseload.map { it.licenceId }).containsExactlyInAnyOrder(
        1,
        2,
        null,
        null,
      )
      assertThat(caseload.map { it.prisonerNumber }).containsExactlyInAnyOrder(
        "A1234AA",
        "AB1234H",
        "AB1234I",
        "AB1234J",
      )
      with(caseload[2]) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12352")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }
    }

    private fun stubSearchPrisonersByNomisId(releaseDate: String, sled: String, tused: String) {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        readFile("staff-create-case-load-prisoners").replace(
          "\$releaseDate",
          releaseDate,
        ).replace("\$sled", sled).replace("\$tused", tused),
      )
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
      val accessResponse = """
      {
        "access": [
          {
            "crn": "X12348",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12349",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12353",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          }
        ]
      }
      """.trimIndent()
      // Given
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffendersByTeam("teamC")
      deliusMockServer.stubGetCheckUserAccess(accessResponse)
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      val sled = LocalDate.now().plusDays(11).format(DateTimeFormatter.ISO_DATE)
      val tused = LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_DATE)
      stubSearchPrisonersByNomisId(releaseDate, sled, tused)
      prisonApiMockServer.stubGetCourtOutcomes()
      prisonApiMockServer.stubGetSentenceAndRecallTypes(3)

      // When
      val result = webTestClient.post()
        .uri(GET_TEAM_CREATE_CASELOAD)
        .bodyValue(TeamCaseloadRequest(listOf("teamA", "teamB"), listOf("teamC")))
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()

      // Then
      result.expectStatus().isEqualTo(OK.value())

      val caseload = result.expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCreateCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(3)
      assertThat(caseload.map { it.prisonerNumber }).containsExactlyInAnyOrder(
        "AB1234E",
        "AB1234F",
        "AB1234G",
      )
      with(caseload[1]) {
        assertThat(kind).isEqualTo(LicenceKind.CRD)
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(prisonerNumber).isEqualTo("AB1234E")
      }
      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12353")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }
    }

    private fun stubSearchPrisonersByNomisId(releaseDate: String, sled: String, tused: String) {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        readFile("team-create-case-load-prisoners").replace(
          "\$releaseDate",
          releaseDate,
        ).replace("\$sled", sled).replace("\$tused", tused),
      )
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

    @Test
    @Sql(
      "classpath:test_data/seed-variation-licence-for-staff-vary-caseload.sql",
    )
    fun `Successfully retrieve staff vary caseload`() {
      val accessResponse = """
      {
        "access": [
           {
            "crn": "X12348",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12351",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12352",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          },
          {
            "crn": "X12353",
            "userExcluded": false,
            "userRestricted": false
          }
        ]
      }
      """.trimIndent()
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)
      deliusMockServer.stubGetCheckUserAccess(accessResponse)
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

      val caseload = webTestClient.get()
        .uri(GET_STAFF_VARY_CASELOAD)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCreateCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(2)
      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12352")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }

      with(caseload[1]) {
        assertThat(crnNumber).isEqualTo("X12348")
        assertThat(prisonerNumber).isEqualTo("AB1234E")
      }
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
      val accessResponse = """
      {
        "access": [
           {
            "crn": "X12348",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12351",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12352",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12353",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          }
        ]
      }
      """.trimIndent()
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffendersByTeam("teamC")
      deliusMockServer.stubGetCheckUserAccess(accessResponse)

      val caseload = webTestClient.post()
        .uri(GET_TEAM_VARY_CASELOAD)
        .bodyValue(TeamCaseloadRequest(listOf("teamA", "teamB"), listOf("teamC")))
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ComCreateCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(2)
      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12353")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }

      with(caseload[1]) {
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
