package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate

class ComIntegrationTest : IntegrationTestBase() {
  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Given a staff member and the teams they are in, search for offenders within their teams`() {
    deliusMockServer.stubGetTeamManagedCases()
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(2)
    assertThat(offender)
      .extracting {
        tuple(
          it?.name, it?.crn, it?.nomisId, it?.comName, it?.comStaffCode, it?.teamName, it?.releaseDate, it?.licenceId,
          it?.licenceType, it?.licenceStatus, it?.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "CRN1",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          LocalDate.now(),
          1L,
          LicenceType.AP,
          LicenceStatus.IN_PROGRESS,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams where the offender does not already have a licence`() {
    deliusMockServer.stubGetTeamManagedCases()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(2)
    assertThat(offender)
      .extracting {
        tuple(
          it?.name, it?.crn, it?.nomisId, it?.comName, it?.comStaffCode, it?.teamName, it?.releaseDate, it?.licenceId,
          it?.licenceType, it?.licenceStatus, it?.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "CRN1",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          prisonerSearchApiMockServer.nextWorkingDate(),
          null,
          LicenceType.AP,
          LicenceStatus.TIMED_OUT,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams with no results from team caseload`() {
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultList = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody

    assertThat(resultList?.results?.size).isEqualTo(0)
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams with no results from prisoner search`() {
    prisonApiMockServer.stubGetCourtOutcomes()
    deliusMockServer.stubGetTeamManagedCases()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIdsNoResult()

    val resultList = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody

    assertThat(resultList?.results?.size).isEqualTo(0)
  }

  @Test
  fun `Given an offender not on probation and their licences is ACTIVE AND CRD and ARD in future When search for offender Then offender should be part of the search results `() {
    deliusMockServer.stubGetTeamManagedCases()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(2)
    assertThat(offender)
      .extracting {
        tuple(
          it?.name, it?.crn, it?.nomisId, it?.comName, it?.comStaffCode, it?.teamName, it?.releaseDate, it?.licenceId,
          it?.licenceType, it?.licenceStatus, it?.isOnProbation,
        )
      }
      .isEqualTo(
        tuple(
          "Test Surname",
          "CRN1",
          "A1234AA",
          "Staff Surname",
          "A01B02C",
          "Test Team",
          prisonerSearchApiMockServer.nextWorkingDate(),
          null,
          LicenceType.AP,
          LicenceStatus.TIMED_OUT,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(2)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given an offender is on probation and their licences is ACTIVE AND CRD and ARD in past When search for offender Then offender should be part of the search results `() {
    deliusMockServer.stubGetTeamManagedCases()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody?.results

    assertThat(resultObject).filteredOn("nomisId", "A1234AD").isNotEmpty
  }

  @Test
  fun `Given an offender is not in jail and licence is INACTIVE When search for offender Then offender should not be part of the search results `() {
    deliusMockServer.stubGetTeamManagedCases()
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetHdcLatest(123L)
    prisonApiMockServer.stubGetCourtOutcomes()

    val resultObject = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ProbationSearchResult::class.java)
      .returnResult().responseBody?.results
    assertThat(resultObject).filteredOn("nomisId", "A1234AE").isEmpty()
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()
    val prisonerSearchApiMockServer = PrisonerSearchMockServer()
    val prisonApiMockServer = PrisonApiMockServer()
    val govUkMockServer = GovUkMockServer()

    val aProbationUserSearchRequest = ProbationUserSearchRequest(
      "Surname",
      1L,
      listOf(
        ProbationSearchSortBy(SearchField.FORENAME, Sort.Direction.ASC),
      ),
    )

    val aLicenceCaseloadSearchRequest = LicenceCaseloadSearchRequest("Surname")

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
      prisonerSearchApiMockServer.start()
      prisonApiMockServer.start()

      govUkMockServer.start()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
      prisonerSearchApiMockServer.stop()
      prisonApiMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
