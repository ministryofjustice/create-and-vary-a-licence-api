package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedConditions

class VariationsIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence-with-new-condition.sql",
  )
  fun `compare a variation to it's parent licence`() {
    val result = webTestClient.get()
      .uri("/variations/2/diff-from-parent")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<VariedConditions>()
      .returnResult().responseBody

    assertThat(result.licenceConditionsAdded).isEqualTo(
      listOf(
        VariedAdditionalCondition(
          category = "Making or maintaining contact with a person",
          condition = "added expanded",
        ),
      ),
    )
    assertThat(result.licenceConditionsRemoved).isEqualTo(
      listOf(
        VariedAdditionalCondition(
          category = "Restriction of residency",
          condition = "expanded text 2",
        ),
      ),
    )
    assertThat(result.licenceConditionsAmended).isEqualTo(
      listOf(
        VariedAdditionalCondition(
          category = "Residence at a specific place",
          condition = "expanded text 1 amended",
        ),
      ),
    )
  }
}
