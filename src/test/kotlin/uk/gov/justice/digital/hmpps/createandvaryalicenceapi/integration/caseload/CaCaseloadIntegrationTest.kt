package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request.CaCaseloadSearch

private const val GET_PRISON_CASELOAD = "/caseload/case-admin/prison-view"
private const val GET_PROBATION_CASELOAD = "/caseload/case-admin/probation-view"
private val caCaseloadSearch = CaCaseloadSearch(prisonCodes = listOf("BAI"), searchString = null)

class CaCaseloadIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

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
      prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
      prisonerSearchMockServer.stubSearchPrisonersByReleaseDate(0)
      communityApiMockServer.stubGetStaffDetailsByUsername()

      val caseload = webTestClient.post()
        .uri(GET_PRISON_CASELOAD)
        .bodyValue(caCaseloadSearch)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ApprovalCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(3)
      with(caseload.first()) {
        assertThat(name).isEqualTo("Prisoner Seven")
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }
    }
  }

  @Nested
  inner class `Get Probation OMU Caseload` {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(GET_PROBATION_CASELOAD)
        .bodyValue(listOf("ABC123"))
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
        .bodyValue(listOf("ABC123"))
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }
  }

  private companion object {
    val govUkMockServer = GovUkMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val probationSearchMockServer = ProbationSearchMockServer()
    val communityApiMockServer = CommunityApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkMockServer.start()
      prisonerSearchMockServer.start()
      probationSearchMockServer.start()
      communityApiMockServer.start()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchMockServer.stop()
      probationSearchMockServer.stop()
      communityApiMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
