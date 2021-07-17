package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.ErrorResponse

class TestDataIntTest : IntegrationTestBase() {
  @Test
  fun `Get a list of test data items`() {
    val result = webTestClient.get()
      .uri("/test/data")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(TestData::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Result returned ${gson.toJson(result)}")
    assertThat(result?.size).isEqualTo(3)
    assertThat(result).extracting("key").containsAll(listOf("A", "B", "C"))
  }

  @Test
  fun `Expect 500 with access denied message when incorrect roles specified`() {
    val result = webTestClient.get()
      .uri("/test/data")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    log.info("Expect 500: Result returned ${gson.toJson(result)}")
    assertThat(result?.userMessage).contains("Access is denied")
  }

  @Test
  fun `Expect 401-Unauthorized when no token supplied`() {
    webTestClient.get()
      .uri("/test/data")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }
}
