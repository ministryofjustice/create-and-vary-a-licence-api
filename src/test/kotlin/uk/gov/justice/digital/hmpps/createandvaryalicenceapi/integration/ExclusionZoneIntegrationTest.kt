package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DocumentApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZonePdfExtract
import java.time.Duration

class ExclusionZoneIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    documentApiMockServer.resetAll()
    documentApiMockServer.stubUploadDocument()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `Uploading an exclusion zone file with document api enabled saves it both locally and remotely`() {
    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    val bodyBuilder = MultipartBodyBuilder()

    bodyBuilder
      .part("file", fileResource.file.readBytes())
      .header("Content-Disposition", "form-data; name=file; filename=" + fileResource.filename)
      .header("Content-Type", "application/pdf")

    webTestClient.post()
      .uri("/exclusion-zone/id/2/condition/id/1/file-upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
      .exchange()
      .expectStatus().isOk

    // Read back the single additional condition from the repository
    val conditions = testRepository.findUploadSummary(1)
    assertThat(conditions).hasSize(1)

    val uploadFile =
      ExclusionZonePdfExtract.fromMultipartFile(MockMultipartFile("file", fileResource.contentAsByteArray))

    // Check that file contents are sent to the document api
    val fullSizeUuid = documentApiMockServer.verifyUploadedDocument(
      fileWasUploaded = uploadFile.fullSizeImage,
      withMetadata = mapOf("licenceId" to "2", "additionalConditionId" to "1", "kind" to "fullSizeImage"),
    )
    val thumbnailUuid = documentApiMockServer.verifyUploadedDocument(
      fileWasUploaded = uploadFile.thumbnailImage,
      withMetadata = mapOf("licenceId" to "2", "additionalConditionId" to "1", "kind" to "thumbnail"),
    )
    val pdfUuid = documentApiMockServer.verifyUploadedDocument(
      fileWasUploaded = fileResource.contentAsByteArray,
      withMetadata = mapOf("licenceId" to "2", "additionalConditionId" to "1", "kind" to "pdf"),
    )

    // Check that the upload summary values are present shows against this additional condition
    val uploadSummary = conditions.first()
    assertThat(uploadSummary.uploadDetailId).isGreaterThan(0)
    assertThat(uploadSummary.filename).isEqualTo(fileResource.filename)
    assertThat(uploadSummary.fileType).isEqualTo("application/pdf")
    assertThat(uploadSummary.imageType).isEqualTo("image/png")
    assertThat(uploadSummary.imageSize).isEqualTo(uploadFile.fullSizeImage.size)
    assertThat(uploadSummary.thumbnailImageDsUuid).isEqualTo(thumbnailUuid.toString())
    assertThat(uploadSummary.description?.trim()).isEqualTo("Description")

    // Check that the upload detail values are also stored and referenced by the ID column in the summary
    val uploadDetail = testRepository.findUploadDetailById(uploadSummary.uploadDetailId)
    assertThat(uploadDetail).isNotNull
    assertThat(uploadDetail!!.licenceId).isEqualTo(2)
    assertThat(uploadDetail.fullSizeImageDsUuid).isEqualTo(fullSizeUuid.toString())
    assertThat(uploadDetail.originalDataDsUuid).isEqualTo(pdfUuid.toString())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  fun `Uploading an exclusion zone file will remove previous uploaded file for same condition`() {
    // Given
    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    val bodyBuilder = MultipartBodyBuilder()
    documentApiMockServer.stubDeleteDocuments()

    bodyBuilder
      .part("file", fileResource.file.readBytes())
      .header("Content-Disposition", "form-data; name=file; filename=" + fileResource.filename)
      .header("Content-Type", "application/pdf")

    assertThat(testRepository.findUploadSummaryById(1)).isNotNull
    assertThat(testRepository.findUploadDetailById(1)).isNotNull

    // When
    val result = webTestClient.post()
      .uri("/exclusion-zone/id/2/condition/id/1/file-upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
      .exchange()

    // Then
    result.expectStatus().isOk

    val summary = testRepository.findUploadSummary(1)
    assertThat(summary).isNotEmpty
    assertThat(summary).hasSize(1)
    assertThat(summary[0].id).isEqualTo(4)

    assertThat(testRepository.findUploadSummaryById(1)).isNull()
    assertThat(testRepository.findAllUploadSummary()).hasSize(3)

    val detail = testRepository.findUploadDetail(1)
    assertThat(detail).isNotEmpty
    assertThat(detail).hasSize(1)
    assertThat(detail[0].id).isEqualTo(4)

    assertThat(testRepository.findUploadDetailById(1)).isNull()
    assertThat(testRepository.findAllUploadDetail()).hasSize(3)

    testRepository.findUploadDetailById(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `exclusion zone upload is role-protected`() {
    val fileResource = ClassPathResource("Test_map_2021-12-06_112550.pdf")
    val bodyBuilder = MultipartBodyBuilder()

    bodyBuilder
      .part("file", fileResource.file.readBytes())
      .header("Content-Disposition", "form-data; name=file; filename=" + fileResource.filename)
      .header("Content-Type", "application/pdf")

    webTestClient.post()
      .uri("/exclusion-zone/id/2/condition/id/1/file-upload")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
      .exchange()
      .expectStatus().isForbidden

    documentApiMockServer.verifyUploadedDocument(didHappenXTimes = 0)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
  )
  fun `get full-sized image for exclusion zone upload`() {
    // Given document service has the file uploaded to it
    documentApiMockServer.stubDownloadDocumentFile(
      withUUID = "44f8163c-6c97-4ff2-932b-ae24feb0c112",
      document = byteArrayOf(9, 9, 9),
    )

    // When I request the Image
    val result = webTestClient.get()
      .uri("/exclusion-zone/id/2/condition/id/1/full-size-image")
      .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.IMAGE_JPEG)
      .expectBody().returnResult()

    // Then I get back the image that was previously uploaded to the document service
    assertThat(result.responseBody).isEqualTo(byteArrayOf(9, 9, 9))
  }

  @Test
  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  fun `Delete exclusion zone`() {
    documentApiMockServer.stubDeleteDocuments()

    // Given
    webTestClient.delete()
      .uri("/licence/id/2/additional-condition/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isNoContent

    documentApiMockServer.verifyDelete("20ca047a-0ae6-4c09-8b97-e3f211feb733")

    assertThat(testRepository.findUploadDetail(2)).isEmpty()
    assertThat(testRepository.findUploadSummary(2)).isEmpty()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
  )
  fun `get full-sized image for exclusion zone is role-protected`() {
    val result = webTestClient.get()
      .uri("/exclusion-zone/id/2/condition/id/1/full-size-image")
      .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus().isForbidden

    val body = result.expectBody(ErrorResponse::class.java).returnResult()
    assertThat(body.responseBody?.status).isEqualTo(HttpStatus.FORBIDDEN.value())
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()
    val documentApiMockServer = DocumentApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      documentApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      documentApiMockServer.stop()
    }
  }
}
