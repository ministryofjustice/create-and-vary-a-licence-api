package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.ProbationSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicenceCaseloadSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.LicenceCaseload
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User

class ComIntegrationTest : IntegrationTestBase() {

  @Value("\${hmpps.community.api.url}")
  val communityApiWiremockUrl: String = ""

  @Value("\${hmpps.probationsearch.api.url}")
  val probationSearchApiWiremockUrl: String = ""

  @Test
  fun `Get team codes for user with valid staff identifier`() {
    communityApiMockServer.stubGetTeamCodesForUser()

    val result = webTestClient.get()
      .uri("$communityApiWiremockUrl/secure/staff/staffIdentifier/123456")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(User::class.java)
      .returnResult().responseBody

    assertThat(result?.teams?.size).isEqualTo(1)
    assertThat(result?.teams)
      .extracting("code")
      .containsExactly("A01B02")
  }
  @Test
  fun `Get error response for user with invalid staff identifier`() {
    communityApiMockServer.stubGetTeamCodesForInvalidUser()

    val result = webTestClient.get()
      .uri("$communityApiWiremockUrl/secure/staff/staffIdentifier/123456")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.status).isEqualTo(HttpStatus.NOT_FOUND.value())
    assertThat(result?.developerMessage).isEqualTo("This is a message")
  }

  @Test
  fun `Search for offenders on a staff member's caseload`() {
    probationSearchApiMockServer.stubPostLicenceCaseloadByTeam(Gson().toJson(aLicenceCaseloadSearchRequest))

    val result = webTestClient.post()
      .uri("$probationSearchApiWiremockUrl/licence-caseload/by-team")
      .bodyValue(aLicenceCaseloadSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_SEARCH")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCaseload::class.java)
      .returnResult().responseBody

    assertThat(result?.content?.size).isEqualTo(1)
    assertThat(result?.content)
      .extracting<Tuple> { Tuple.tuple(it.name.forename, it.name.surname) }
      .containsAll(
        listOf(
          Tuple.tuple("Test", "Surname"),
        )
      )
  }

  @Test
  fun `Search for offenders on a staff member's caseload with no results`() {
    probationSearchApiMockServer.stubPostLicenceCaseloadByTeamNoResult(Gson().toJson(aLicenceCaseloadSearchRequest))

    val result = webTestClient.post()
      .uri("$probationSearchApiWiremockUrl/licence-caseload/by-team")
      .bodyValue(aLicenceCaseloadSearchRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_SEARCH")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceCaseload::class.java)
      .returnResult().responseBody

    assertThat(result?.content?.size).isEqualTo(0)
  }

  private companion object {
    val communityApiMockServer = CommunityApiMockServer()
    val probationSearchApiMockServer = ProbationSearchMockServer()

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
