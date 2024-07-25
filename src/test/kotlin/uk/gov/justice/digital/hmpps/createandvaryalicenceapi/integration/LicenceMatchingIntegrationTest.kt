package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
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
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody!!

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
        tuple(6L, LicenceStatus.REJECTED),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licences matches - by list of staff identifiers`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        MatchLicencesRequest(
          staffId = listOf(125, 126),
        ),
      )
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
        tuple(3L, "C1234CC", LicenceStatus.ACTIVE),
        tuple(4L, "C1234DD", LicenceStatus.APPROVED),
        tuple(5L, "C1234EE", LicenceStatus.IN_PROGRESS),
        tuple(6L, "C1234FF", LicenceStatus.REJECTED),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licence matches - by list of staff identifiers and statuses`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        MatchLicencesRequest(
          staffId = listOf(125),
          status = listOf(LicenceStatus.ACTIVE),
        ),
      )
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
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licence matches - by list of prisons and statuses`() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .bodyValue(
        MatchLicencesRequest(
          prison = listOf("MDI", "BMI"),
          status = listOf(LicenceStatus.APPROVED, LicenceStatus.SUBMITTED),
        ),
      )
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
    "classpath:test_data/seed-matching-candidates.sql",
  )
  fun `Get licence matches - no matching filters`() {
    val result = webTestClient.post()
      .uri("/licence/match")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .bodyValue(
        MatchLicencesRequest(
          prison = listOf("XXX"),
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
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.size).isEqualTo(6)
    assertThat(result)
      .extracting<Tuple> {
        tuple(it.licenceId, it.conditionalReleaseDate)
      }
      .containsExactly(
        tuple(6L, LocalDate.parse("2036-04-28")),
        tuple(5L, LocalDate.parse("2035-04-28")),
        tuple(4L, LocalDate.parse("2034-04-28")),
        tuple(3L, LocalDate.parse("2033-04-28")),
        tuple(2L, LocalDate.parse("2032-04-28")),
        tuple(1L, LocalDate.parse("2031-04-28")),
      )
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
