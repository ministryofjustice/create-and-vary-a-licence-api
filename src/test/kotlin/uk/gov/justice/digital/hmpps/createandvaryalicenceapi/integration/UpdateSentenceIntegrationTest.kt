package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class UpdateSentenceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update sentence dates`() {
    prisonApiMockServer.stubGetHdcLatest()

    webTestClient.put()
      .uri("/licence/id/1/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.parse("2023-09-11"),
          actualReleaseDate = LocalDate.parse("2023-09-11"),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceEndDate = LocalDate.parse("2024-09-11"),
          licenceStartDate = LocalDate.parse("2023-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.conditionalReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.actualReleaseDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.sentenceStartDate).isEqualTo(LocalDate.parse("2021-09-11"))
    assertThat(result?.sentenceEndDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.licenceStartDate).isEqualTo(LocalDate.parse("2023-09-11"))
    assertThat(result?.licenceExpiryDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionStartDate).isEqualTo(LocalDate.parse("2024-09-11"))
    assertThat(result?.topupSupervisionExpiryDate).isEqualTo(LocalDate.parse("2025-09-11"))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-3.sql",
  )
  fun `Update sentence dates should set license status to inactive when the offender has a new future release date`() {
    prisonApiMockServer.stubGetHdcLatest()

    webTestClient.put()
      .uri("/licence/id/3/sentence-dates")
      .bodyValue(
        UpdateSentenceDatesRequest(
          conditionalReleaseDate = LocalDate.now().plusDays(5),
          actualReleaseDate = LocalDate.now().plusDays(2),
          sentenceStartDate = LocalDate.parse("2021-09-11"),
          sentenceEndDate = LocalDate.parse("2024-09-11"),
          licenceStartDate = LocalDate.parse("2023-09-11"),
          licenceExpiryDate = LocalDate.parse("2024-09-11"),
          topupSupervisionStartDate = LocalDate.parse("2024-09-11"),
          topupSupervisionExpiryDate = LocalDate.parse("2025-09-11"),
        ),
      )
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

    assertThat(result?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
  }

  private companion object {
    val prisonApiMockServer = PrisonApiMockServer()
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
      govUkApiMockServer.stop()
    }
  }
}
