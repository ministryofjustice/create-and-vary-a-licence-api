package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOmuEmailRequest

class OmuContactIntegrationTest : IntegrationTestBase() {

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.put()
      .uri("/omu/FPI/contact/email")
      .bodyValue(UpdateOmuEmailRequest(email = "test@testing.com"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.put()
      .uri("/omu/fpi/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-omu-contact-data.sql",
  )
  fun `query for OMU email address`() {
    webTestClient.get()
      .uri("/omu/FPI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `query for none existent OMU email address`() {
    webTestClient.get()
      .uri("/omu/FPI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `query for creating new OMU email address`() {
    webTestClient.put()
      .uri("/omu/FPI/contact/email")
      .bodyValue(UpdateOmuEmailRequest(email = "test@testing.com"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  @Sql(
    "classpath:test_data/seed-omu-contact-data.sql",
  )
  fun `query for updating new OMU email address`() {
    webTestClient.put()
      .uri("/omu/FPI/contact/email")
      .bodyValue(UpdateOmuEmailRequest(email = "test@testing.com"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  @Sql(
    "classpath:test_data/seed-omu-contact-data.sql",
  )
  fun `query for deleting OMU email address`() {
    webTestClient.delete()
      .uri("/omu/FPI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `query for deleting none existent OMU email address`() {
    webTestClient.delete()
      .uri("/omu/FPI/contact/email")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
  }
}
