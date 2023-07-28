package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.EnrichedProbationSearchResults
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchField
import java.time.LocalDate

class ComIntegrationTest : IntegrationTestBase() {
  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Given a staff member and the teams they are in, search for offenders within their teams`() {
    communityApiMockServer.stubGetTeamCodesForUser()
    probationSearchApiMockServer.stubPostLicenceCaseloadByTeam(Gson().toJson(aLicenceCaseloadSearchRequest))

    val resultObject = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EnrichedProbationSearchResults::class.java)
      .returnResult().responseBody

    val resultsList = resultObject?.results
    val offender = resultsList?.first()
    val inPrisonCount = resultObject?.inPrisonCount
    val onProbationCount = resultObject?.onProbationCount

    assertThat(resultsList?.size).isEqualTo(1)
    assertThat(offender)
      .extracting { tuple(it?.name, it?.comName, it?.teamName, it?.releaseDate, it?.licenceStatus, it?.isOnProbation) }
      .isEqualTo(
        tuple(
          "Test Surname",
          "Staff Surname",
          "Test Team",
          LocalDate.parse("2022-02-12"),
          LicenceStatus.IN_PROGRESS,
          false,
        ),
      )

    assertThat(inPrisonCount).isEqualTo(1)
    assertThat(onProbationCount).isEqualTo(0)
  }

  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams with no results`() {
    communityApiMockServer.stubGetTeamCodesForUser()
    probationSearchApiMockServer.stubPostLicenceCaseloadByTeamNoResult(Gson().toJson(aLicenceCaseloadSearchRequest))

    val resultList = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EnrichedProbationSearchResults::class.java)
      .returnResult().responseBody

    assertThat(resultList?.results?.size).isEqualTo(0)
  }

  private companion object {
    val communityApiMockServer = CommunityApiMockServer()
    val probationSearchApiMockServer = ProbationSearchMockServer()

    val aProbationUserSearchRequest = ProbationUserSearchRequest(
      "Surname",
      1L,
      listOf(
        ProbationSearchSortBy(SearchField.FORENAME, SearchDirection.ASC),
      ),
    )

    val aLicenceCaseloadSearchRequest = LicenceCaseloadSearchRequest(
      listOf("A01B02"),
      "Surname",
      listOf(
        ProbationSearchSortByRequest("name.forename", "asc"),
      ),
    )

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      communityApiMockServer.start()
      probationSearchApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      communityApiMockServer.stop()
      probationSearchApiMockServer.stop()
    }
  }
}
