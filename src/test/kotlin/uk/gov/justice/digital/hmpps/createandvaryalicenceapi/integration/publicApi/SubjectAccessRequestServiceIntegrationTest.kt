package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceKinds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.SarContent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType

class SubjectAccessRequestServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-sar-content-licence-id.sql",
  )
  fun `Get SAR records by PRN`() {
    val resultList = webTestClient.get()
      .uri("/public/subject-access-request?prn=A1234AA")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(SarContent::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(1)

    val result = resultList?.first()

    assertThat(result?.content?.licences?.first()).extracting(
      "kind",
      "nomsId",
      "bookingId",
      "createdByUsername",
    )
      .isEqualTo(
        listOf(
          LicenceKinds.CRD,
          "A1234AA",
          12345L,
          "test-client",
        ),
      )

    assertThat(result?.content?.auditEvents?.first()).extracting(
      "licenceId",
      "username",
      "eventType",
      "summary",
      "detail",
    )
      .isEqualTo(
        listOf(
          1L,
          "USER",
          AuditEventType.USER_EVENT,
          "Summary1",
          "Detail1",
        ),
      )

    assertThat(result?.content?.licencesEvents?.get(2)).extracting(
      "licenceId",
      "eventType",
      "username",
      "eventDescription",
    )
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.CREATED,
          "Bob Smith",
          "Licence created2",
        ),
      )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-sar-content-licence-id.sql",
  )
  fun `Get sarContent by PRN is role protected`() {
    val result = webTestClient.get()
      .uri("/public/subject-access-request?prn=A1234AA")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
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
