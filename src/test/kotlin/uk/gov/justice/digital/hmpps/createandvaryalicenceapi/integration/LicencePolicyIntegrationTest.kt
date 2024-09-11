package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets.UTF_8

class LicencePolicyIntegrationTest : IntegrationTestBase() {

  fun policy(v: String) =
    this.javaClass.getResourceAsStream("/test_data/policy_conditions/policy$v.json")!!.bufferedReader(UTF_8).readText()

  @Test
  fun policyV1() {
    webTestClient.get()
      .uri("/licence-policy/version/1.0")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json(policy("V1"), true)
  }

  @Test
  fun policyV2() {
    webTestClient.get()
      .uri("/licence-policy/version/2.0")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json(policy("V2"), true)
  }

  @Test
  fun policyV2_1() {
    webTestClient.get()
      .uri("/licence-policy/version/2.1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json(policy("V2_1"), true)
  }

  @Test
  fun policyV3() {
    webTestClient.get()
      .uri("/licence-policy/version/3.0")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .json(policy("V3"), true)
  }
}
