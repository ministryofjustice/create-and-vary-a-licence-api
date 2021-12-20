package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import java.time.Duration

class ExclusionZoneIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var additionalConditionRepository: AdditionalConditionRepository

  @Autowired
  lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
  }

  @Test
  @Sql(
    "classpath:test_data/clear-all-licences.sql",
    "classpath:test_data/seed-licence-id-2.sql"
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
    val conditions = additionalConditionRepository.findById(1).map { condition -> condition.additionalConditionUploadSummary }.orElseThrow()
    assertThat(conditions).hasSize(1)
    val uploadCondition = conditions.first()

    log.info("AdditionalConditionSummary")
    log.info("ConditionId - ${uploadCondition.id}")
    log.info("UploadDetailId - ${uploadCondition.uploadDetailId}")
    log.info("UploadedTime - ${uploadCondition.uploadedTime}")
    log.info("FileSize - ${uploadCondition.fileSize}")
    log.info("FileName - ${uploadCondition.filename}")
    log.info("FileType - ${uploadCondition.fileType}")
    log.info("Description - ${uploadCondition.description}")

    // Check that the upload summary values are present shows against this additional condition
    assertThat(uploadCondition.uploadDetailId).isGreaterThan(0)
    assertThat(uploadCondition.filename).isEqualTo(fileResource.filename)
    assertThat(uploadCondition.fileType).isEqualTo("application/pdf")
    assertThat(uploadCondition.thumbnailImage).isNotEmpty

    // Check that the upload detail values are also stored and referenced by the ID column in the summary
    val detailRow = additionalConditionUploadDetailRepository.findById(uploadCondition.uploadDetailId).orElseThrow()

    log.info("AdditionalConditionDetail")
    log.info("Id - ${detailRow.id}")
    log.info("LicenceId - ${detailRow.licenceId}")
    log.info("AdditionalConditionId - ${detailRow.additionalConditionId}")
    log.info("Length original - ${detailRow.originalData?.size}")
    log.info("Length full size - ${detailRow.fullSizeImage?.size}")

    assertThat(detailRow.fullSizeImage).isNotEmpty
    assertThat(detailRow.originalData).isEqualTo(fileResource.inputStream.readAllBytes())
  }
}
