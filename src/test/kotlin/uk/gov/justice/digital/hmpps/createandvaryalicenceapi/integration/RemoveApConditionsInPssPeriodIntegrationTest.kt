package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository

class RemoveApConditionsInPssPeriodIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-4.sql",
  )
  fun `Update sentence dates`() {
    webTestClient.post()
      .uri("/run-remove-ap-conditions-job")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/3")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    val additionalConditions = result?.additionalLicenceConditions

    // additionalConditions should be deleted i.e. empty if licence is in PSS period
    assertThat(additionalConditions).isEmpty()

    val additionalPssConditions = result?.additionalPssConditions

    // additionalPssConditions should not be deleted
    assertThat(additionalPssConditions).isNotEmpty()

    // additionalPssConditions length should be equal to 2
    assertThat(additionalPssConditions?.size).isEqualTo(2)

    // additionalPssCondition first record id should be equal to 2
    assertThat(additionalPssConditions?.first()?.id).isEqualTo(2)

    val result1 = webTestClient.get()
      .uri("/licence/id/4")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result1?.additionalLicenceConditions).isEmpty()
    assertThat(result1?.additionalPssConditions?.size).isEqualTo(1)
    assertThat(result1?.additionalPssConditions?.first()?.id).isEqualTo(7)

    val result2 = webTestClient.get()
      .uri("/licence/id/5")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result2?.additionalLicenceConditions).isNotEmpty()
    assertThat(result2?.additionalLicenceConditions?.size).isEqualTo(1)
    assertThat(result2?.additionalLicenceConditions?.first()?.id).isEqualTo(8)
    assertThat(result2?.additionalPssConditions?.size).isEqualTo(1)
    assertThat(result2?.additionalPssConditions?.first()?.id).isEqualTo(9)
  }
}
