package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence

class UnapprovedLicenceIntegrationTest : IntegrationTestBase() {
  @Test
  @Sql(
    "classpath:test_data/clear-all-data.sql",
    "classpath:test_data/seed-community-offender-manager.sql",
    "classpath:test_data/seed-audit-events.sql",
    "classpath:test_data/seed-submitted-licences.sql"
  )
  fun `Get unapproved licences`() {
    val result = webTestClient.get()
      .uri("/edited-licences-unapproved-by-crd")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("SYSTEM_USER", "ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(UnapprovedLicence::class.java)
      .returnResult().responseBody

    assertThat(result).size().isEqualTo(3)

    assertThat(result?.get(0)?.crn).isEqualTo("100")
    assertThat(result?.get(0)?.forename).isEqualTo("jim")
    assertThat(result?.get(0)?.surname).isEqualTo("smith")
    assertThat(result?.get(0)?.comFirstName).isEqualTo("Test")
    assertThat(result?.get(0)?.comLastName).isEqualTo("Client")
    assertThat(result?.get(0)?.comEmail).isEqualTo("testClient@probation.gov.uk")

    assertThat(result?.get(1)?.crn).isEqualTo("300")
    assertThat(result?.get(1)?.forename).isEqualTo("terry")
    assertThat(result?.get(1)?.surname).isEqualTo("towel")
    assertThat(result?.get(1)?.comFirstName).isEqualTo("Adam")
    assertThat(result?.get(1)?.comLastName).isEqualTo("AAA")
    assertThat(result?.get(1)?.comEmail).isEqualTo("testAAA@probation.gov.uk")

    assertThat(result?.get(2)?.crn).isEqualTo("600")
    assertThat(result?.get(2)?.forename).isEqualTo("prisoner")
    assertThat(result?.get(2)?.surname).isEqualTo("six")
    assertThat(result?.get(2)?.comFirstName).isEqualTo("Test")
    assertThat(result?.get(2)?.comLastName).isEqualTo("Client")
    assertThat(result?.get(2)?.comEmail).isEqualTo("testClient@probation.gov.uk")
  }
}
