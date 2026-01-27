package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.CaCaseloadSearch
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration

private const val GET_PROBATION_CASELOAD = "/caseload/case-admin/probation-view"
private const val SEARCH_PRISONERS_CA_CASELOAD = "/caseload/case-admin/case-search"
private val caCaseloadSearch = CaCaseloadSearch(prisonCodes = setOf("BAI"), searchString = null)

class CaCaseloadIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
    govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Nested
  inner class `Get Probation OMU Caseload` {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(GET_PROBATION_CASELOAD)
        .bodyValue(caCaseloadSearch)
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
        .uri(GET_PROBATION_CASELOAD)
        .bodyValue(caCaseloadSearch)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    @Sql(
      "classpath:test_data/seed-ca-caseload-licences.sql",
    )
    fun `Successfully retrieve ca caseload`() {
      prisonerSearchMockServer.stubSearchPrisonersByReleaseDate(0)
      prisonApiMockServer.getHdcStatuses()
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
      deliusMockServer.stubGetManagers()
      deliusMockServer.stubGetStaffDetailsByUsername()

      val caseload = webTestClient.post()
        .uri(GET_PROBATION_CASELOAD)
        .bodyValue(caCaseloadSearch)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<CaCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(2)
      with(caseload.first()) {
        assertThat(name).isEqualTo("Person Five")
        assertThat(prisonerNumber).isEqualTo("A1234AE")
        assertThat(licenceStatus).isEqualTo(LicenceStatus.ACTIVE)
        assertThat(tabType).isNull()
        assertThat(isInHardStopPeriod).isFalse()
        assertThat(kind).isEqualTo(LicenceKind.CRD)
      }
    }
  }

  @Nested
  inner class CaseAdminSearchTest {
    val request = PrisonUserSearchRequest(
      query = "Person",
      prisonCaseloads = setOf("BAI"),
    )

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(SEARCH_PRISONERS_CA_CASELOAD)
        .bodyValue(request)
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
        .uri(SEARCH_PRISONERS_CA_CASELOAD)
        .bodyValue(request)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    @Sql(
      "classpath:test_data/seed-ca-caseload-licences.sql",
    )
    fun successfullyRetrievePrisonProbationCases() {
      prisonerSearchMockServer.stubSearchPrisonersByReleaseDate(0)
      prisonApiMockServer.getHdcStatuses()
      prisonApiMockServer.stubGetCourtOutcomes()
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagersWithoutUserDetails()

      val caseload = webTestClient.post()
        .uri(SEARCH_PRISONERS_CA_CASELOAD)
        .bodyValue(request)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<PrisonCaseAdminSearchResult>())
        .returnResult().responseBody!!

      assertThat(caseload.inPrisonResults).hasSize(5)
      assertThat(caseload.onProbationResults).hasSize(2)
      assertThat(caseload.attentionNeededResults).hasSize(0)

      with(caseload.inPrisonResults.first()) {
        assertThat(name).isEqualTo("Person Two")
        assertThat(prisonerNumber).isEqualTo("A1234AB")
        assertThat(licenceStatus).isEqualTo(LicenceStatus.SUBMITTED)
        assertThat(tabType).isEqualTo(CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS)
        assertThat(isInHardStopPeriod).isFalse()
      }

      with(caseload.onProbationResults.first()) {
        assertThat(name).isEqualTo("Person Five")
        assertThat(prisonerNumber).isEqualTo("A1234AE")
        assertThat(licenceStatus).isEqualTo(LicenceStatus.ACTIVE)
        assertThat(tabType).isNull()
        assertThat(isInHardStopPeriod).isFalse()
      }
    }

    @Test
    @Sql(
      "classpath:test_data/seed-ca-caseload-licences.sql",
    )
    fun successfullyRetrieveTimeServedCaseWithNomisLicence() {
      prisonerSearchMockServer.stubSearchPrisonersByReleaseDate(0)
      prisonApiMockServer.getHdcStatuses()
      prisonApiMockServer.stubGetCourtOutcomes()
      prisonApiMockServer.stubGetSentenceAndRecallTypes()
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
      deliusMockServer.stubGetStaffDetailsByUsername()
      deliusMockServer.stubGetManagersWithoutUserDetails()

      val caseload = webTestClient.post()
        .uri(SEARCH_PRISONERS_CA_CASELOAD)
        .bodyValue(request)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<PrisonCaseAdminSearchResult>())
        .returnResult().responseBody!!

      assertThat(caseload.inPrisonResults).hasSize(5)
      with(caseload.inPrisonResults[2]) {
        assertThat(name).isEqualTo("Test5 Person5")
        assertThat(prisonerNumber).isEqualTo("A1234AG")
        assertThat(licenceStatus).isEqualTo(LicenceStatus.TIMED_OUT)
        assertThat(tabType).isEqualTo(CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS)
        assertThat(isInHardStopPeriod).isFalse()
        assertThat(lastWorkedOnBy).isEqualTo("Test Client")
        assertThat(hasNomisLicence).isTrue()
      }
    }
  }

  private companion object {
    val govUkMockServer = GovUkMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkMockServer.start()
      prisonerSearchMockServer.start()
      deliusMockServer.start()
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      deliusMockServer.stop()
      govUkMockServer.stop()
      prisonApiMockServer.stop()
    }
  }
}
