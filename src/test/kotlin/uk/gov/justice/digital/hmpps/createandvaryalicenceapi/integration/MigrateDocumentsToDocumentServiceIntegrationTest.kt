package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document.DocumentApiClient
import java.time.Duration

class MigrateDocumentsToDocumentServiceIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var docApiClient: DocumentApiClient

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-post-documents.sql",
  )
  fun `Given there is a full size image when POST run-copy-documents then documents should be copied to document service`() {
    webTestClient.post()
      .uri("/run-copy-documents/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
  }

  @Test
  @Sql(
    "classpath:test_data/seed-post-documents.sql",
  )
  fun `Given there is a raw data pdf when POST run-copy-documents then documents service should be used to copy to document service`() {
    webTestClient.post()
      .uri("/run-copy-documents/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
    val metadata =
      DocumentMetaData(
        "19",
        "216",
        "EXCLUSION_ZONE_MAP_PDF",
      )
    verify(docApiClient, times(1)).postDocument(
      any(),
      any(),
      eq(MediaType.APPLICATION_PDF),
      eq(metadata),
      eq("EXCLUSION_ZONE_MAP"),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-post-documents.sql",
  )
  fun `Given there is a full size image when POST run-copy-documents then documents service should be used to copy to document service`() {
    webTestClient.post()
      .uri("/run-copy-documents/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
    val metadata =
      DocumentMetaData(
        "19",
        "216",
        "EXCLUSION_ZONE_MAP_FULL_IMG",
      )
    verify(docApiClient, times(1)).postDocument(
      any(),
      any(),
      eq(MediaType.IMAGE_JPEG),
      eq(metadata),
      eq("EXCLUSION_ZONE_MAP"),
    )
  }

  @Test
  @Sql("classpath:test_data/seed-post-documents.sql")
  fun `Given full size image and it is copied to document service when POST run-remove-copied-documents then full size image removed`() {
    webTestClient.post()
      .uri("/run-remove-copied-documents/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val detailRow = additionalConditionUploadDetailRepository.findById(1).orElseThrow()
    Assertions.assertThat(detailRow.fullSizeImage).isNull()
  }

  @Test
  @Sql("classpath:test_data/seed-post-documents.sql")
  fun `Given full size image and it is copied to document service when POST run-remove-copied-documents then pdf documents removed`() {
    webTestClient.post()
      .uri("/run-remove-copied-documents/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val detailRow = additionalConditionUploadDetailRepository.findById(1).orElseThrow()
    Assertions.assertThat(detailRow.originalData).isNull()
  }

  @Test
  @Sql("classpath:test_data/seed-post-documents.sql")
  fun `Given full size image and it is copied to document service when POST run-remove-copied-documents then thumbnail is removed`() {
    webTestClient.post()
      .uri("/run-remove-copied-documents/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val summary = additionalConditionRepository.findById(1).orElseThrow()
    Assertions.assertThat(summary.additionalConditionUploadSummary.stream().findFirst().get().thumbnailImage).isNull()
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
