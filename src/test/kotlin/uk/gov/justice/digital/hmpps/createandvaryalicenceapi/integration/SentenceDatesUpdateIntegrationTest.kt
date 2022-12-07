package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CONDITION_CODE_FOR_14B
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class SentenceDatesUpdateIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var notifyService: NotifyService

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Happy path`() {
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
        )
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
    "classpath:test_data/seed-licence-id-3.sql"
  )
  fun `Should set licence status to inactive when the offender has a new future release date`() {
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
        )
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

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
    "classpath:test_data/seed-omu-contact-data.sql"
  )
  fun `Should update monitoring end date when has condition 14b and there is a new future release date`() {
    prisonApiMockServer.stubGetHdcLatest()

    webTestClient.put()
      .uri("/licence/id/1/additional-conditions")
      .bodyValue(anAdditionalConditionsRequestIncluding14b)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val condition14b = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody!!.additionalLicenceConditions[1]

    assertThat(condition14b.data[0].value).isEqualTo("Saturday 25 February 2023")

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
        )
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val condition14bAfter = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody!!.additionalLicenceConditions[1]

    assertThat(condition14bAfter.data[0].value).isEqualTo("Wednesday 11 September 2024")

    verify(notifyService).sendElectronicMonitoringEndDatesChangedEmail(
      "1",
      emailAddress = "test.OMU@testing.com",
      prisonerFirstName = "Bob",
      prisonerLastName = "Mortimer",
      prisonNumber = "A1234AA",
      reasonForChange = "release date and licence end date"
    )
  }

  private companion object {
    val prisonApiMockServer = PrisonApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      prisonApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      prisonApiMockServer.stop()
    }

    val anAdditionalConditionsRequestIncluding14b = AdditionalConditionsRequest(
      additionalConditions = listOf(
        AdditionalCondition(code = "code1", category = "category", sequence = 0, text = "text"),
        AdditionalCondition(
          code = CONDITION_CODE_FOR_14B,
          category = "Electronic monitoring",
          sequence = 1,
          text = "text"
        ),
      ),
      conditionType = "AP"
    )
  }
}
