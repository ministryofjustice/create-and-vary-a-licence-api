package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.SarAuditEventType
import uk.gov.justice.hmpps.kotlin.sar.Attachment
import java.time.LocalDateTime

class SubjectAccessRequestServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CVL_ADMIN", "ROLE_SAR_DATA_ACCESS"])
  @Sql(
    "classpath:test_data/seed-sar-content-licence-id.sql",
  )
  fun `Get HmppsSubjectAccessRequestContent records by PRN`(role: String) {
    val resultList = webTestClient.get()
      .uri("/subject-access-request?prn=A1234AA")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(HmppsSubjectAccessRequestContentTestDto::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(1)

    val content = resultList.first().content

    assertThat(content.licences).hasSize(2)
    with(content.licences[0]) {
      assertThat(id).isEqualTo(1L)
      assertThat(prisonNumber).isEqualTo("A1234AA")
      assertThat(bookingId).isEqualTo(12345L)
      assertThat(createdByUsername).isEqualTo("test-client")
    }
    with(content.licences[1]) {
      assertThat(id).isEqualTo(2L)
      assertThat(prisonNumber).isEqualTo("A1234AA")
      assertThat(bookingId).isEqualTo(123456L)
      assertThat(createdByUsername).isEqualTo("test-client")
    }

    assertThat(content.auditEvents).hasSize(5)
    with(content.auditEvents[0]) {
      assertThat(licenceId).isEqualTo(1L)
      assertThat(username).isEqualTo("USER")
      assertThat(eventType).isEqualTo(SarAuditEventType.USER_EVENT)
      assertThat(summary).isEqualTo("Summary1")
      assertThat(detail).isEqualTo("Detail1")
    }

    assertThat(content.timeServedExternalRecords).hasSize(2)
    with(content.timeServedExternalRecords[0]) {
      assertThat(prisonNumber).isEqualTo("A1234AA")
      assertThat(reason).isEqualTo("Time served licence created in NOMIS")
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(dateCreated).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0))
      assertThat(dateLastUpdated).isEqualTo(LocalDateTime.of(2024, 6, 1, 11, 0))
    }
    with(content.timeServedExternalRecords[1]) {
      assertThat(prisonNumber).isEqualTo("A1234AA")
      assertThat(reason).isEqualTo("Some other time served licence created in NOMIS")
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(dateCreated).isEqualTo(LocalDateTime.of(2024, 6, 2, 10, 0))
      assertThat(dateLastUpdated).isEqualTo(LocalDateTime.of(2024, 6, 2, 11, 0))
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["ROLE_CVL_ADMIN", "ROLE_SAR_DATA_ACCESS"])
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
  )
  fun `Get hmppsSubjectAccessRequestContent with exclusion zone`(role: String) {
    val resultList = webTestClient.get()
      .uri("/subject-access-request?prn=A1234AA")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(HmppsSubjectAccessRequestContentTestDto::class.java)
      .returnResult().responseBody

    assertThat(resultList?.size).isEqualTo(1)

    val result = resultList?.first()

    val summary = result!!.content.licences[0].additionalLicenceConditions[0].uploadSummary[0]
    assertThat(summary.attachmentNumber).isEqualTo(0)
    assertThat(summary.filename).isEqualTo("Test-file.pdf")
    assertThat(summary.imageType).isEqualTo("image/png")
    assertThat(summary.fileSize).isEqualTo(23456)
    assertThat(summary.description).isEqualTo("Description")

    assertThat(result.attachments).isEqualTo(
      listOf(
        Attachment(
          attachmentNumber = 0,
          name = "Description",
          contentType = "image/png",
          url = "http://localhost:8089/public/licences/2/conditions/1/image-upload",
          filename = "Test-file.pdf",
          filesize = 23456,
        ),
        Attachment(
          attachmentNumber = 1,
          name = "Description",
          contentType = "image/png",
          url = "http://localhost:8089/public/licences/2/conditions/2/image-upload",
          filename = "Test-file-2.pdf",
          filesize = 23456,
        ),
      ),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-sar-content-licence-id.sql",
  )
  fun `Get hmppsSubjectAccessRequestContent by PRN is role protected`() {
    val result = webTestClient.get()
      .uri("/subject-access-request?prn=A1234AA")
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
