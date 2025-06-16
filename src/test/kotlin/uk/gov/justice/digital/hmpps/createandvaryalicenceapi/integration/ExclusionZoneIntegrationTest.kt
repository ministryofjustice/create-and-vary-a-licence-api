package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ExclusionZoneUploadsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.ExclusionZoneUploadFile
import java.time.Duration

class ExclusionZoneIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository

  @Autowired
  lateinit var exclusionZoneUploadsRepository: ExclusionZoneUploadsRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    documentApiMockServer.stubUploadDocument()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
  )
  fun `Upload an exclusion zone file`() {
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
    val conditions = additionalConditionRepository.findById(1)
      .map { it.additionalConditionUploadSummary }
      .orElseThrow()
    assertThat(conditions).hasSize(1)

    val uploadFile = ExclusionZoneUploadFile(MockMultipartFile("file", fileResource.contentAsByteArray))

    // Check that the upload summary values are present shows against this additional condition
    val uploadSummary = conditions.first()
    assertThat(uploadSummary.uploadDetailId).isGreaterThan(0)
    assertThat(uploadSummary.filename).isEqualTo(fileResource.filename)
    assertThat(uploadSummary.fileType).isEqualTo("application/pdf")
    assertThat(uploadSummary.thumbnailImage).isEqualTo(uploadFile.thumbnailImage)
    assertThat(uploadSummary.description?.trim()).isEqualTo("Description")

    // Check that the upload detail values are also stored and referenced by the ID column in the summary
    val uploadDetail = additionalConditionUploadDetailRepository.findById(uploadSummary.uploadDetailId).orElseThrow()
    assertThat(uploadDetail.licenceId).isEqualTo(2)
    assertThat(uploadDetail.fullSizeImage).isEqualTo(uploadFile.fullSizeImage)
    assertThat(uploadDetail.originalData).isEqualTo(fileResource.inputStream.readAllBytes())

    // Check that file contents are sent to the document api
    val fullSizeUuid = documentApiMockServer.verifyUploadedDocument(
      fileWasUploaded = uploadFile.fullSizeImage!!,
      withMetadata = mapOf("licenceId" to "2", "additionalConditionId" to "1", "kind" to "fullSizeImage"),
    )
    val thumbnailUuid = documentApiMockServer.verifyUploadedDocument(
      fileWasUploaded = uploadFile.thumbnailImage!!,
      withMetadata = mapOf("licenceId" to "2", "additionalConditionId" to "1", "kind" to "thumbnail"),
    )
    val pdfUuid = documentApiMockServer.verifyUploadedDocument(
      fileWasUploaded = fileResource.contentAsByteArray,
      withMetadata = mapOf("licenceId" to "2", "additionalConditionId" to "1", "kind" to "pdf"),
    )

    // Check that we are saving reference to the uuids of the docs sent to the remote api
    assertThat(exclusionZoneUploadsRepository.count()).isEqualTo(1)
    val exclusionZoneUpload = exclusionZoneUploadsRepository.findById(1).get()
    assertThat(exclusionZoneUpload.licence.id).isEqualTo(2L)
    assertThat(exclusionZoneUpload.additionalCondition.id).isEqualTo(1L)
    assertThat(exclusionZoneUpload.pdfId).isEqualTo(pdfUuid)
    assertThat(exclusionZoneUpload.thumbnailId).isEqualTo(thumbnailUuid)
    assertThat(exclusionZoneUpload.fullImageId).isEqualTo(fullSizeUuid)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
  )
  fun `remove an exclusion zone upload`() {
    webTestClient.put()
      .uri("/exclusion-zone/id/2/condition/id/1/remove-upload")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/licence/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    val additionalCondition = result?.additionalLicenceConditions?.first()
    assertThat(additionalCondition?.uploadSummary).isEmpty()
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
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
  )
  fun `remove exclusion zone upload is role-protected`() {
    val result = webTestClient.put()
      .uri("/exclusion-zone/id/2/condition/id/1/remove-upload")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
      .exchange()
      .expectStatus().isForbidden

    val body = result.expectBody(ErrorResponse::class.java).returnResult()
    assertThat(body.responseBody?.status).isEqualTo(HttpStatus.FORBIDDEN.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-2.sql",
    "classpath:test_data/add-upload-to-licence-id-2.sql",
  )
  fun `get full-sized image for exclusion zone upload`() {
    val result = webTestClient.get()
      .uri("/exclusion-zone/id/2/condition/id/1/full-size-image")
      .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(result.expectHeader().contentType(MediaType.IMAGE_JPEG)).isNotNull
    assertThat(result.expectBody()).isNotNull
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
