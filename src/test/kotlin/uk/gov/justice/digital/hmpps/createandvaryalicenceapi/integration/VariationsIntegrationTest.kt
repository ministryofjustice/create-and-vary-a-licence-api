package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EditVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariationChangeResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.VariedAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

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
      .expectBody<VariationChangeResponse>()
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
    assertThat(result.hasUpdatedCurfewHours).isFalse()
    assertThat(result.hasUpdatedCurfewAddress).isFalse()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-submitted-licence.sql",
  )
  fun `edits a submitted variation`() {
    webTestClient.post()
      .uri("/variations/id/2/edit")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .bodyValue(EditVariationRequest(username = "TEST_USER"))
      .exchange()
      .expectStatus().isOk

    val variation = testRepository.findLicence(2)
    assertThat(variation.statusCode).isEqualTo(LicenceStatus.VARIATION_IN_PROGRESS)

    val audit = testRepository.findFirstAuditEvent(2)
    assertThat(audit.summary).isEqualTo("Licence variation changed to in progress for Test User2")
  }
}
