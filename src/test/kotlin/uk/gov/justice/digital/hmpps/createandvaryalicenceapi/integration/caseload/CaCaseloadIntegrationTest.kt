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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCaseLoad
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.CaCaseloadSearch
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration

private const val GET_PRISON_CASELOAD = "/caseload/case-admin/prison-view"
private const val GET_PROBATION_CASELOAD = "/caseload/case-admin/probation-view"
private val caCaseloadSearch = CaCaseloadSearch(prisonCodes = listOf("BAI"), searchString = null)

class CaCaseloadIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
    govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Nested
  inner class `Get Prison OMU Caseload` {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(GET_PRISON_CASELOAD)
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
        .uri(GET_PRISON_CASELOAD)
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
      probationSearchMockServer.stubSearchForPersonByNomsNumberForGetApprovalCaseload()
      communityApiMockServer.stubGetStaffDetailsByUsername()

      val caseload = webTestClient.post()
        .uri(GET_PRISON_CASELOAD)
        .bodyValue(caCaseloadSearch)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<CaCaseLoad>())
        .returnResult().responseBody!!

      assertThat(caseload.cases).hasSize(4)
      with(caseload.cases.first()) {
        assertThat(name).isEqualTo("harry hope")
        assertThat(prisonerNumber).isEqualTo("A1234AB")
        assertThat(licenceStatus).isEqualTo(LicenceStatus.SUBMITTED)
        assertThat(tabType).isEqualTo(CaViewCasesTab.FUTURE_RELEASES)
        assertThat(isInHardStopPeriod).isFalse()
      }
    }
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
      probationSearchMockServer.stubSearchForPersonByNomsNumberForGetApprovalCaseload()
      communityApiMockServer.stubGetStaffDetailsByUsername()

      val caseload = webTestClient.post()
        .uri(GET_PROBATION_CASELOAD)
        .bodyValue(caCaseloadSearch)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<CaCaseLoad>())
        .returnResult().responseBody!!

      assertThat(caseload.cases).hasSize(2)
      with(caseload.cases.first()) {
        assertThat(name).isEqualTo("prisoner five")
        assertThat(prisonerNumber).isEqualTo("A1234AE")
        assertThat(licenceStatus).isEqualTo(LicenceStatus.ACTIVE)
        assertThat(tabType).isNull()
        assertThat(isInHardStopPeriod).isFalse()
      }
    }
  }

  private companion object {
    val govUkMockServer = GovUkMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val probationSearchMockServer = ProbationSearchMockServer()
    val communityApiMockServer = CommunityApiMockServer()
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkMockServer.start()
      prisonerSearchMockServer.start()
      probationSearchMockServer.start()
      communityApiMockServer.start()
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      probationSearchMockServer.stop()
      communityApiMockServer.stop()
      govUkMockServer.stop()
      prisonApiMockServer.stop()
    }
  }
}
