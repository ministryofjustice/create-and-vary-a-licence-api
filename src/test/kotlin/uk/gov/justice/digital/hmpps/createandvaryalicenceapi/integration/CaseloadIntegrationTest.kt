package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerNumbers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ReleaseDateSearch
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

private const val GET_PRISONER = "/prisoner-search/nomisid/A1234AA"
private const val FIND_PRISONERS = "/prisoner-search/prisoner-numbers"
private const val FIND_PRISONERS_BY_RELEASE_DATE = "/prisoner-search/release-date-by-prison?size=2000"

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
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
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

      webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND.value())
    }

    @Test
    fun success() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

      val caseloadItem = webTestClient.get()
        .uri(GET_PRISONER)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(CaseloadItem::class.java)
        .returnResult().responseBody!!

      with(caseloadItem) {
        assertThat(prisoner).isNotNull
        with(cvl) {
          assertThat(licenceType).isEqualTo(LicenceType.AP)
          assertThat(hardStopDate).isNotNull
          assertThat(hardStopWarningDate).isNotNull
        }
      }
    }
  }

  @Nested
  inner class GetPrisoners {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(FIND_PRISONERS)
        .bodyValue(PrisonerNumbers(listOf("A1234AA")))
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
        .uri(FIND_PRISONERS)
        .bodyValue(PrisonerNumbers(listOf("A1234AA")))
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun `not present`() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIdsNoResult()

      val caseload = webTestClient.post()
        .uri(FIND_PRISONERS)
        .bodyValue(PrisonerNumbers(listOf("A1234AA")))
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBodyList(CaseloadItem::class.java)
        .returnResult().responseBody!!

      assertThat(caseload).isEmpty()
    }

    @Test
    fun success() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

      val caseload = webTestClient.post()
        .uri(FIND_PRISONERS)
        .bodyValue(PrisonerNumbers(listOf("A1234AA")))
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBodyList(CaseloadItem::class.java)
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(5)
      with(caseload.first()) {
        assertThat(prisoner).isNotNull()
        with(cvl) {
          assertThat(licenceType).isEqualTo(LicenceType.AP)
          assertThat(hardStopDate).isNotNull
          assertThat(hardStopWarningDate).isNotNull
        }
      }
    }
  }

  @Nested
  inner class GetPrisonersByReleaseDate {
    val releaseDateSearch = ReleaseDateSearch(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2), setOf("MDI"))

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri(FIND_PRISONERS_BY_RELEASE_DATE)
        .bodyValue(releaseDateSearch)
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
        .uri(FIND_PRISONERS_BY_RELEASE_DATE)
        .bodyValue(releaseDateSearch)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun success() {
      prisonerSearchApiMockServer.stubSearchPrisonersByReleaseDate()

      val caseload = webTestClient.post()
        .uri(FIND_PRISONERS_BY_RELEASE_DATE)
        .bodyValue(releaseDateSearch)
        .accept(APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBodyList(CaseloadItem::class.java)
        .returnResult().responseBody!!

      assertThat(caseload).hasSize(5)
      with(caseload.first()) {
        assertThat(prisoner).isNotNull()
        with(cvl) {
          assertThat(licenceType).isEqualTo(LicenceType.AP)
          assertThat(hardStopDate).isNotNull
          assertThat(hardStopWarningDate).isNotNull
        }
      }
    }
  }

  private companion object {
    val prisonerSearchApiMockServer = PrisonerSearchMockServer()
    val govUkMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonerSearchApiMockServer.start()
      govUkMockServer.start()
      govUkMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonerSearchApiMockServer.stop()
      govUkMockServer.stop()
    }
  }
}
