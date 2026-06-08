package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurrentPrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

private const val GET_PRISONER = "/prisoner-search/nomisid/A1234AA"
private const val GET_PROBATION_CASE = "/caseload/probation-case/A1234AA"

class CaseloadIntegrationTest : IntegrationTestBase() {
  @Nested
  inner class GetPrisoner {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
        .exchange()
        .expectStatus().isForbidden
        .expectStatus().isEqualTo(FORBIDDEN.value())
        .expectBody<ErrorResponse>()
        .returnResult().responseBody

      assertThat(result.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun `not present`() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIdsNoResult()
      prisonApiMockServer.stubGetCourtOutcomes()

      webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND.value())
    }

    @Test
    fun success() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        conditionalReleaseDate = LocalDate.now(),
        sentenceStartDate = LocalDate.now().plusDays(1),
      )
      prisonApiMockServer.stubGetCourtOutcomes()
      hdcApiMockServer.stubGetHdcStatuses(
        listOf(
          CurrentPrisonerHdcStatus(123, HdcStatus.APPROVED),
        ),
      )
      deliusMockServer.stubGetOffenderManagerWithNomsId()

      val caseloadItem = webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody<PrisonerWithCvlFields>()
        .returnResult().responseBody

      with(caseloadItem) {
        assertThat(prisoner).isNotNull
        with(cvl) {
          assertThat(licenceType).isEqualTo(LicenceType.AP)
          assertThat(hardStopDate).isNotNull
          assertThat(hardStopWarningDate).isNotNull
          assertThat(licenceStartDate).isEqualTo(prisoner.confirmedReleaseDate)
          assertThat(isTimeServed).isEqualTo(false)
        }
      }
    }

    @Test
    fun checkDateFormats() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(
        conditionalReleaseDate = LocalDate.now(),
        sentenceStartDate = LocalDate.now().plusDays(1),
      )
      prisonApiMockServer.stubGetCourtOutcomes()
      deliusMockServer.stubGetOffenderManagerWithNomsId()

      webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.cvl.hardStopDate").value<String> { assertThat(it).matches("\\d{2}/\\d{2}/\\d{4}") }
        .jsonPath("$.prisoner.dateOfBirth").value<String> { assertThat(it).matches("\\d{4}-\\d{2}-\\d{2}") }
    }
  }

  @Nested
  inner class GetProbationCase {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.get()
        .uri(GET_PROBATION_CASE)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
        .exchange()
        .expectStatus().isForbidden
        .expectStatus().isEqualTo(FORBIDDEN.value())
        .expectBody<ErrorResponse>()
        .returnResult().responseBody

      assertThat(result.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.get()
        .uri(GET_PROBATION_CASE)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun success() {
      deliusMockServer.stubGetOffenderManager()
      deliusMockServer.stubGetOffenderManagerWithNomsId()

      val probationCase = webTestClient.get()
        .uri(GET_PROBATION_CASE)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody<ProbationCase>()
        .returnResult().responseBody

      assertThat(probationCase).isEqualTo(ProbationCase(crn = "X12345", comAllocated = true, prisonNumber = "A1234AA"))
    }
  }

  private companion object {
    @RegisterExtension
    val prisonApiMockServer = PrisonApiMockServer()

    @RegisterExtension
    val prisonerSearchApiMockServer = PrisonerSearchMockServer()

    @RegisterExtension
    val deliusMockServer = DeliusMockServer()
    val hdcApiMockServer = HdcApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      hdcApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      hdcApiMockServer.stop()
    }
  }
}
