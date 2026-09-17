package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.MatchLicencesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class LicenceMatchingIntegrationTest : IntegrationTestBase() {

  @Test
  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
  )
  fun `Find approved licences`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .bodyValue(MatchLicencesRequest(status = listOf(LicenceStatus.APPROVED)))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = cvlRoles()))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result.size).isEqualTo(5)
    assertThat(result.map { it.licenceId }).containsExactly(1L, 2L, 3L, 4L, 5L)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licences matches - no filters`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(MatchLicencesRequest())
      .headers(setAuthorisation(roles = cvlRoles()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.licenceStatus) }
      .containsExactly(
        tuple(1L, LicenceStatus.SUBMITTED),
        tuple(2L, LicenceStatus.SUBMITTED),
        tuple(3L, LicenceStatus.ACTIVE),
        tuple(4L, LicenceStatus.APPROVED),
        tuple(5L, LicenceStatus.IN_PROGRESS),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licences matches - by list of prison numbers`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        MatchLicencesRequest(
          nomsId = listOf("C1234CC", "C1234DD", "C1234EE", "C1234FF"),
        ),
      )
      .headers(setAuthorisation(roles = cvlRoles()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result)
      .extracting<Tuple> { tuple(it.licenceId, it.nomisId, it.licenceStatus) }
      .containsExactly(
        tuple(3L, "C1234CC", LicenceStatus.ACTIVE),
        tuple(4L, "C1234DD", LicenceStatus.APPROVED),
        tuple(5L, "C1234EE", LicenceStatus.IN_PROGRESS),
      )
  }

  @ParameterizedTest(name = "Get licences matches using role {0}")
  @MethodSource("cvlRoles")
  @Sql("classpath:test_data/seed-matching-candidates.sql")
  fun `Get licences matches using role`(role: String) {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(MatchLicencesRequest(nomsId = listOf("C1234CC")))
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus().isOk

    result.expectStatus().isOk
  }

  @Test
  @Sql(
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licence matches - no matching filters`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = cvlRoles()))
      .bodyValue(
        MatchLicencesRequest(
          nomsId = listOf("XXX"),
          status = listOf(LicenceStatus.APPROVED, LicenceStatus.SUBMITTED, LicenceStatus.IN_PROGRESS),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result).isEmpty()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licence matches - sort by conditional release date`() {
    val result = webTestClient.post()
      .uri("/licence/match?sortBy=conditionalReleaseDate&sortOrder=DESC")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(MatchLicencesRequest())
      .headers(setAuthorisation(roles = cvlRoles()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result)
      .extracting<Tuple> {
        tuple(it.licenceId, it.conditionalReleaseDate)
      }
      .containsExactly(
        tuple(5L, LocalDate.parse("2035-04-28")),
        tuple(4L, LocalDate.parse("2034-04-28")),
        tuple(3L, LocalDate.parse("2033-04-28")),
        tuple(2L, LocalDate.parse("2032-04-28")),
        tuple(1L, LocalDate.parse("2031-04-28")),
      )
  }
}
