package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class LicenceMatchingIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-matching-candidates.sql"
  )
  fun `Get licences matches - no filters`() {
    val result = webTestClient.get()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(6)
    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.licenceStatus) }
      .contains(
        tuple(1L, LicenceStatus.SUBMITTED),
        tuple(2L, LicenceStatus.SUBMITTED),
        tuple(3L, LicenceStatus.ACTIVE),
        tuple(4L, LicenceStatus.APPROVED),
        tuple(5L, LicenceStatus.IN_PROGRESS),
        tuple(6L, LicenceStatus.REJECTED)
      )
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-matching-candidates.sql"
  )
  fun `Get licences matches - by list of staff identifiers`() {
    val result = webTestClient.get()
      .uri("/licence/match?staffId=125&staffId=126")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(4)
    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.nomisId, it.licenceStatus) }
      .contains(
        tuple(1L, "A1234AA", LicenceStatus.SUBMITTED),
        tuple(2L, "B1234BB", LicenceStatus.SUBMITTED),
        tuple(3L, "C1234CC", LicenceStatus.ACTIVE),
        tuple(4L, "C1234DD", LicenceStatus.APPROVED),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-matching-candidates.sql"
  )
  fun `Get licence matches - by list of staff identifiers and statuses`() {
    val result = webTestClient.get()
      .uri("/licence/match?staffId=125&status=ACTIVE")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(1)
    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.nomisId, it.licenceStatus, it.surname) }
      .containsExactly(tuple(3L, "C1234CC", LicenceStatus.ACTIVE, "Cookson"))
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-matching-candidates.sql"
  )
  fun `Get licence matches - by list of prisons and statuses`() {
    val result = webTestClient.get()
      .uri("/licence/match?prison=MDI&prison=BMI&status=APPROVED&status=SUBMITTED")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(3)
    assertThat(result)
      .extracting<Tuple> {
        tuple(it.licenceId, it.licenceStatus, it.nomisId, it.surname, it.forename, it.prisonCode, it.prisonDescription)
      }
      .contains(
        tuple(1L, LicenceStatus.SUBMITTED, "A1234AA", "Alda", "Alan", "MDI", "Moorland HMP"),
        tuple(2L, LicenceStatus.SUBMITTED, "B1234BB", "Bobson", "Bob", "MDI", "Moorland HMP"),
        tuple(4L, LicenceStatus.APPROVED, "C1234DD", "Harcourt", "Kate", "BMI", "Birmingham HMP"),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-matching-candidates.sql"
  )
  fun `Get licence matches - no matching filters`() {
    val result = webTestClient.get()
      .uri("/licence/match?prison=XXX&status=APPROVED&status=SUBMITTED&status=IN_PROGRESS")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result).isEmpty()
  }
}
