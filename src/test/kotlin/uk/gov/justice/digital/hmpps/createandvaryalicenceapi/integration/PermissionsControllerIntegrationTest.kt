package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.typeReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessDetails
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessRestrictionType.EXCLUDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessRestrictionType.NONE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessRestrictionType.RESTRICTED

private const val GET_CASE_ACCESS_DETAILS = "/probation-staff/CRN1/permissions"

class PermissionsControllerIntegrationTest : IntegrationTestBase() {

  @Nested
  inner class GetCaseAccessDetails {
    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.get()
        .uri(GET_CASE_ACCESS_DETAILS)
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
        .uri(GET_CASE_ACCESS_DETAILS)
        .accept(APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }

    @Test
    fun `Successfully retrieve the access details for a case with no restrictions`() {
      deliusMockServer.stubGetCaseAccessDetails()

      val caseAccessDetails = webTestClient.get()
        .uri(GET_CASE_ACCESS_DETAILS)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<CaseAccessDetails>())
        .returnResult().responseBody!!

      assertThat(caseAccessDetails.type).isEqualTo(NONE)
    }

    @Test
    fun `Correctly retrieve the access details for restricted case`() {
      deliusMockServer.stubGetCaseAccessDetails(

        userRestricted = true,
        restrictionMessage = "This access has been restricted",
      )

      val caseAccessDetails = webTestClient.get()
        .uri(GET_CASE_ACCESS_DETAILS)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<CaseAccessDetails>())
        .returnResult().responseBody!!

      assertThat(caseAccessDetails).isEqualTo(CaseAccessDetails(RESTRICTED, "This access has been restricted"))
    }

    @Test
    fun `Correctly retrieve the access details for excluded case`() {
      deliusMockServer.stubGetCaseAccessDetails(
        userExcluded = true,
        exclusionMessage = "This access has been excluded",
      )

      val caseAccessDetails = webTestClient.get()
        .uri(GET_CASE_ACCESS_DETAILS)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<CaseAccessDetails>())
        .returnResult().responseBody!!

      assertThat(caseAccessDetails).isEqualTo(CaseAccessDetails(EXCLUDED, "This access has been excluded"))
    }

    @Test
    fun `Correctly retrieve the access details for excluded and restricted case`() {
      deliusMockServer.stubGetCaseAccessDetails(
        userExcluded = true,
        userRestricted = true,
        exclusionMessage = "This access has been excluded, but is also restricted so this mustn't be displayed",
        restrictionMessage = "This access has been restricted, this message must be displayed",
      )

      val caseAccessDetails = webTestClient.get()
        .uri(GET_CASE_ACCESS_DETAILS)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isEqualTo(OK.value())
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody(typeReference<CaseAccessDetails>())
        .returnResult().responseBody!!

      assertThat(caseAccessDetails).isEqualTo(CaseAccessDetails(RESTRICTED, "This access has been restricted, this message must be displayed"))
    }
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
    }
  }
}
