package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import java.nio.charset.StandardCharsets

class PublicLicencePolicyServiceIntegrationTest : IntegrationTestBase() {

  fun policy(v: String) =
    this.javaClass.getResourceAsStream("/test_data/publicApi/licencePolicy/policy$v.json")!!.bufferedReader(StandardCharsets.UTF_8).readText()

  @Nested
  inner class `Get policy by version number` {

    @Test
    fun `get policy v1 by version number`() {
      webTestClient.get()
        .uri("/public/policy/1.0")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json(policy("V1"), true)
    }

    @Test
    fun `get policy v2 by version number`() {
      webTestClient.get()
        .uri("/public/policy/2.0")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json(policy("V2"), true)
    }

    @Test
    fun `get policy v2_1 by version number`() {
      webTestClient.get()
        .uri("/public/policy/2.1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json(policy("V2_1"), true)
    }

    @Test
    fun `Get policy by version number is role protected`() {
      val result = webTestClient.get()
        .uri("/public/policy/2.1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      Assertions.assertThat(result?.userMessage).contains("Access Denied")
    }
  }
}
