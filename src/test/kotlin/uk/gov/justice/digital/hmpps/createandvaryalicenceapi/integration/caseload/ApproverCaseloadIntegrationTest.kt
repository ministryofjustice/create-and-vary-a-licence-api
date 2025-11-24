package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.caseload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ApproverSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ApproverSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCommunityManager
import java.time.LocalDate

class ApproverCaseloadIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Nested
  inner class `Get Approval Caseload` {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri("/caseload/prison-approver/approval-needed")
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
        .uri("/caseload/prison-approver/approval-needed")
        .bodyValue(listOf("ABC123"))
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    @Sql(
      "classpath:test_data/seed-submitted-licences.sql",
    )
    fun `Successfully retrieve approval caseload in the ascending release date and ascending name order`() {
      deliusMockServer.stubGetManagers(
        excludeUserInfo = true,
        managers = listOf(
          createCommunityManager(1, "A1234BC"),
          createCommunityManager(2, "A1234AB"),
          createCommunityManager(3, "C1234BC"),
          createCommunityManager(4, "B1234BC"),
        ),
      )

      val caseload = webTestClient.post()
        .uri("/caseload/prison-approver/approval-needed")
        .bodyValue(listOf("ABC"))
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ApprovalCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(4)
      with(caseload[0]) {
        assertThat(releaseDate).isNull()
        assertThat(name).isEqualTo("Person Seven")
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }
      with(caseload[1]) {
        assertThat(releaseDate).isEqualTo(LocalDate.of(2022, 1, 1))
        assertThat(name).isEqualTo("Person Two")
        assertThat(prisonerNumber).isEqualTo("A1234AB")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }
      with(caseload[2]) {
        assertThat(releaseDate).isEqualTo(LocalDate.of(2022, 1, 1))
        assertThat(name).isEqualTo("Person Z")
        assertThat(prisonerNumber).isEqualTo("C1234BC")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }
      with(caseload[3]) {
        assertThat(releaseDate).isEqualTo(LocalDate.of(2024, 3, 14))
        assertThat(name).isEqualTo("Person Eight")
        assertThat(prisonerNumber).isEqualTo("B1234BC")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }
    }
  }

  @Nested
  inner class `Get Recently Approved Caseload` {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri("/caseload/prison-approver/recently-approved")
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
        .uri("/caseload/prison-approver/recently-approved")
        .bodyValue(listOf("ABC123"))
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    @Sql(
      "classpath:test_data/seed-recently-approved-licences.sql",
    )
    fun `Successfully retrieve recently approved caseload in the desc approval date and ascending name order`() {
      deliusMockServer.stubGetManagers(
        excludeUserInfo = true,
        managers = listOf(
          createCommunityManager(1, "F2504MG"),
          createCommunityManager(2, "B1234BB"),
          createCommunityManager(3, "C1234BC"),
        ),
      )

      val caseload = webTestClient.post()
        .uri("/caseload/prison-approver/recently-approved")
        .bodyValue(listOf("MDI"))
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<List<ApprovalCase>>())
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(3)
      with(caseload[0]) {
        assertThat(name).isEqualTo("Person Eight")
        assertThat(approvedOn?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(prisonerNumber).isEqualTo("F2504MG")
        assertThat(approvedBy).isNotNull()
        assertThat(approvedOn).isNotNull()
        assertThat(releaseDate).isAfter(LocalDate.now().minusDays(14))
      }
      with(caseload[1]) {
        assertThat(name).isEqualTo("Person A")
        assertThat(approvedOn?.toLocalDate()).isEqualTo(LocalDate.now().minusDays(5))
        assertThat(prisonerNumber).isEqualTo("B1234BB")
        assertThat(approvedBy).isNotNull()
        assertThat(approvedOn).isNotNull()
        assertThat(releaseDate).isAfter(LocalDate.now().minusDays(14))
      }
      with(caseload[2]) {
        assertThat(name).isEqualTo("Person Z")
        assertThat(approvedOn?.toLocalDate()).isEqualTo(LocalDate.now().minusDays(5))
        assertThat(prisonerNumber).isEqualTo("C1234BC")
        assertThat(approvedBy).isNotNull()
        assertThat(approvedOn).isNotNull()
        assertThat(releaseDate).isAfter(LocalDate.now().minusDays(14))
      }
    }
  }

  @Nested
  inner class ApproverSearchTest {
    val request = ApproverSearchRequest(
      prisonCaseloads = listOf("ABC", "AAA"),
      query = "Person",
    )

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri("/caseload/prison-approver/case-search")
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
        .uri("/caseload/prison-approver/case-search")
        .bodyValue(request)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    @Sql(
      "classpath:test_data/seed-approver-caseload-licences.sql",
    )
    fun successfullyRetrieveSubmittedAndRecentlyApprovedCases() {
      deliusMockServer.stubGetManagers(
        excludeUserInfo = true,
        managers = listOf(
          createCommunityManager(1, "A1234BC"),
          createCommunityManager(2, "B1234BC"),
          createCommunityManager(3, "F2504MG"),
          createCommunityManager(4, "B1234BB"),
        ),
      )

      val result = webTestClient.post()
        .uri("/caseload/prison-approver/case-search")
        .bodyValue(request)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<ApproverSearchResponse>())
        .returnResult().responseBody!!

      assertThat(result.approvalNeededResponse).hasSize(2)
      assertThat(result.recentlyApprovedResponse).hasSize(2)

      with(result.approvalNeededResponse.first()) {
        assertThat(name).isEqualTo("Person Seven")
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }

      with(result.approvalNeededResponse.last()) {
        assertThat(name).isEqualTo("Person Eight")
        assertThat(prisonerNumber).isEqualTo("B1234BC")
        assertThat(approvedBy).isNull()
        assertThat(approvedOn).isNull()
      }

      with(result.recentlyApprovedResponse.first()) {
        assertThat(name).isEqualTo("Person Eighteen")
        assertThat(prisonerNumber).isEqualTo("F2504MG")
        assertThat(approvedBy).isNotNull()
        assertThat(approvedOn).isNotNull()
        assertThat(releaseDate).isBeforeOrEqualTo(LocalDate.now().minusDays(10))
      }

      with(result.recentlyApprovedResponse.last()) {
        assertThat(name).isEqualTo("Person Ten")
        assertThat(prisonerNumber).isEqualTo("B1234BB")
        assertThat(approvedBy).isNotNull()
        assertThat(approvedOn).isNotNull()
        assertThat(releaseDate).isBeforeOrEqualTo(LocalDate.now().minusDays(10))
      }
    }
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()
    val govUkMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
      govUkMockServer.start()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
