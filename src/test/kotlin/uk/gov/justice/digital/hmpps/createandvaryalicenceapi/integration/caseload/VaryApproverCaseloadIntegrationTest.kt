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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.VaryApproverCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.VaryApproverCaseloadSearchResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.Charsets.UTF_8

private const val DELIUS_STAFF_IDENTIFIER = 3492L
private const val GET_VARY_APPROVER_CASELOAD = "/caseload/vary-approver"
private const val SEARCH_VARY_APPROVER_CASELOAD = "/caseload/vary-approver/case-search"

class VaryApproverCaseloadIntegrationTest : IntegrationTestBase() {

  fun readFile(filename: String): String = this.javaClass.getResourceAsStream("/test_data/integration/caseload/$filename.json")!!.bufferedReader(UTF_8)
    .readText()

  @Nested
  inner class GetVaryApproverCaseload {
    private val caseSearchRequest = VaryApproverCaseloadSearchRequest(probationAreaCode = "N55")

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(GET_VARY_APPROVER_CASELOAD)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
        .bodyValue(caseSearchRequest)
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
        .uri(GET_VARY_APPROVER_CASELOAD)
        .accept(APPLICATION_JSON)
        .bodyValue(caseSearchRequest)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Sql(
      "classpath:test_data/seed-variation-submitted-licence.sql",
    )
    @Test
    fun `Successfully retrieve vary approver caseload`() {
      val accessResponse = """
      {
        "access": [
           {
            "crn": "X12349",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12350",
            "userExcluded": true,
            "userRestricted": false,
            "exclusionMessage": "Access restricted on NDelius"
          }
        ]
      }
      """.trimIndent()
      deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)
      deliusMockServer.stubGetProbationCases()
      deliusMockServer.stubGetManagersWithoutUserDetails()
      deliusMockServer.stubGetCheckUserAccess(accessResponse)
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        readFile("vary-approver-case-load-prisoners").replace(
          "\$releaseDate",
          releaseDate,
        ),
      )
      prisonApiMockServer.stubGetCourtOutcomes()

      val caseload = webTestClient.post()
        .uri(GET_VARY_APPROVER_CASELOAD)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .bodyValue(caseSearchRequest)
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<VaryApproverCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(2)

      with(caseload.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12350")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }

      with(caseload[1]) {
        assertThat(crnNumber).isEqualTo("X12349")
        assertThat(name).isEqualTo("Test2 Person2")
      }
    }
  }

  @Nested
  inner class VaryApproverCaseloadSearch {
    private val caseSearchRequest =
      VaryApproverCaseloadSearchRequest(probationPduCodes = null, probationAreaCode = "N55", searchTerm = "X123")

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(SEARCH_VARY_APPROVER_CASELOAD)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
        .bodyValue(caseSearchRequest)
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
        .uri(SEARCH_VARY_APPROVER_CASELOAD)
        .accept(APPLICATION_JSON)
        .bodyValue(caseSearchRequest)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Sql(
      "classpath:test_data/seed-variation-submitted-licence.sql",
    )
    @Test
    fun `Successfully search for vary approver case`() {
      val accessResponse = """
      {
        "access": [
           {
            "crn": "X12349",
            "userExcluded": false,
            "userRestricted": false
          },
          {
            "crn": "X12350",
            "userExcluded": true,
            "userRestricted": false
          }
        ]
      }
      """.trimIndent()
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagedOffenders(DELIUS_STAFF_IDENTIFIER)
      deliusMockServer.stubGetProbationCases()
      deliusMockServer.stubGetManagersWithoutUserDetails()
      deliusMockServer.stubGetCheckUserAccess(accessResponse)
      val releaseDate = LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_DATE)
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        readFile("vary-approver-case-load-prisoners").replace(
          "\$releaseDate",
          releaseDate,
        ),
      )
      prisonApiMockServer.stubGetCourtOutcomes()

      val result = webTestClient.post()
        .uri(SEARCH_VARY_APPROVER_CASELOAD)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .bodyValue(caseSearchRequest)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<VaryApproverCaseloadSearchResponse>())
        .returnResult().responseBody!!

      assertThat(result.pduCasesResponse).hasSize(0)
      assertThat(result.regionCasesResponse).hasSize(2)

      with(result.regionCasesResponse.first()) {
        assertThat(name).isEqualTo("Access restricted on NDelius")
        assertThat(crnNumber).isEqualTo("X12350")
        assertThat(probationPractitioner.name).isEqualTo("Restricted")
        assertThat(probationPractitioner.staffCode).isEqualTo("Restricted")
        assertThat(isRestricted).isTrue()
      }

      with(result.regionCasesResponse.last()) {
        assertThat(crnNumber).isEqualTo("X12349")
        assertThat(name).isEqualTo("Test2 Person2")
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
