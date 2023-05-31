package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest

class ComIntegrationTest : IntegrationTestBase() {
  @Test
  fun `Given a staff member and the teams they are in, search for offenders within their teams`() {
    communityApiMockServer.stubGetTeamCodesForUser()
    probationSearchApiMockServer.stubPostLicenceCaseloadByTeam(Gson().toJson(aLicenceCaseloadSearchRequest))

    val resultList = webTestClient.post()
      .uri("/com/case-search")
      .bodyValue(aProbationUserSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ProbationSearchResult::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(1)
    assertThat(resultList)
      .extracting<Tuple> { tuple(it.name, it.comName, it.comCode) }
      .contains(
        tuple("Test Surname", "Staff Surname", "A01B02C")
      )
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
      .expectBodyList(ProbationSearchResult::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(0)
  }

  private companion object {
    val communityApiMockServer = CommunityApiMockServer()
    val probationSearchApiMockServer = ProbationSearchMockServer()

    val aProbationUserSearchRequest = ProbationUserSearchRequest(
      "Surname",
      1L
    )

    val aLicenceCaseloadSearchRequest = LicenceCaseloadSearchRequest(
      listOf("A01B02"),
      "Surname",
      ProbationSearchSortByRequest()
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
