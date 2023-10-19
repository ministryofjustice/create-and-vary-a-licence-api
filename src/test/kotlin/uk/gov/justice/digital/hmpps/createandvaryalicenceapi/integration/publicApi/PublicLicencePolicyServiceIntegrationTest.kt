package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.LicencePolicy
import java.nio.charset.StandardCharsets

class PublicLicencePolicyServiceIntegrationTest : IntegrationTestBase() {

  fun policy(v: String) =
    this.javaClass.getResourceAsStream("/test_data/publicApi/licencePolicy/policy$v.json")!!.bufferedReader(StandardCharsets.UTF_8).readText()

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
  fun `Get policy by version number`() {
    val result = webTestClient.get()
      .uri("/public/policy/2.1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicencePolicy::class.java)
      .returnResult().responseBody

    Assertions.assertThat(result?.version).isEqualTo("2.1")

    val anApStandardCondition = result?.conditions?.apConditions?.standard?.first()
    val anApAdditionalCondition = result?.conditions?.apConditions?.additional?.first()
    val aPssStandardCondition = result?.conditions?.pssConditions?.standard?.first()
    val aPssAdditionalCondition = result?.conditions?.pssConditions?.additional?.first()

    Assertions.assertThat(anApStandardCondition)
      .extracting {
        Tuple.tuple(it?.code, it?.text)
      }
      .isEqualTo(
        Tuple.tuple(
          "9ce9d594-e346-4785-9642-c87e764bee37",
          "Be of good behaviour and not behave in a way which undermines the purpose of the licence period.",
        ),
      )

    Assertions.assertThat(anApAdditionalCondition)
      .extracting {
        Tuple.tuple(it?.code, it?.text, it?.category, it?.categoryShort, it?.requiresInput)
      }
      .isEqualTo(
        Tuple.tuple(
          "5db26ab3-9b6f-4bee-b2aa-53aa3f3be7dd",
          "You must reside overnight within [REGION] probation region while of no fixed abode, unless otherwise approved by your supervising officer.",
          "Residence at a specific place",
          null,
          true,
        ),
      )

    Assertions.assertThat(aPssStandardCondition)
      .extracting {
        Tuple.tuple(it?.code, it?.text)
      }
      .isEqualTo(
        Tuple.tuple(
          "b3cd4a30-11fd-4715-9ebb-ed89f5386e1f",
          "Be of good behaviour and not behave in a way that undermines the rehabilitative purpose of the supervision period.",
        ),
      )

    Assertions.assertThat(aPssAdditionalCondition)
      .extracting {
        Tuple.tuple(it?.code, it?.text, it?.category, it?.categoryShort, it?.requiresInput)
      }
      .isEqualTo(
        Tuple.tuple(
          "62c83b80-2223-4562-a195-0670f4072088",
          "Attend [INSERT APPOINTMENT TIME DATE AND ADDRESS], as directed, to address your dependency on, or propensity to misuse, a controlled drug.",
          "Drug appointment",
          null,
          true,
        ),
      )
  }

  @Test
  fun `Get forbidden (403) when incorrect role is supplied`() {
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
