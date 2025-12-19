package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DocumentApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class PublicLicenceServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadRepository

  @Nested
  inner class `Get licences by CRN` {
    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-1.sql",
    )
    fun `Get licences by CRN`() {
      val resultList = webTestClient.get()
        .uri("/public/licence-summaries/crn/CRN1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(PublicLicenceSummary::class.java)
        .returnResult().responseBody

      assertThat(resultList?.size).isEqualTo(1)

      val result = resultList?.first()

      assertThat(result?.id).isEqualTo(1L)
      assertThat(result?.kind).isEqualTo(LicenceKind.CRD)
      assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
      assertThat(result?.policyVersion).isEqualTo(PolicyVersion.V1_0)
      assertThat(result?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
      assertThat(result?.prisonNumber).isEqualTo("A1234AA")
      assertThat(result?.bookingId).isEqualTo(12345L)
      assertThat(result?.crn).isEqualTo("CRN1")
      assertThat(result?.createdByUsername).isEqualTo("test-client")
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-1.sql",
    )
    fun `Get licences by CRN is role protected`() {
      val result = webTestClient.get()
        .uri("/public/licence-summaries/crn/CRN1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  @Nested
  inner class `Get licences by prisoner number` {
    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-1.sql",
    )
    fun `Get licences by prisoner number`() {
      val resultList = webTestClient.get()
        .uri("/public/licence-summaries/prison-number/A1234AA")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_VIEW_LICENCES")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(PublicLicenceSummary::class.java)
        .returnResult().responseBody

      assertThat(resultList?.size).isEqualTo(1)

      val result = resultList?.first()

      assertThat(result?.id).isEqualTo(1L)
      assertThat(result?.kind).isEqualTo(LicenceKind.CRD)
      assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
      assertThat(result?.policyVersion).isEqualTo(PolicyVersion.V1_0)
      assertThat(result?.version).isEqualTo("1.0")
      assertThat(result?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
      assertThat(result?.prisonNumber).isEqualTo("A1234AA")
      assertThat(result?.bookingId).isEqualTo(12345L)
      assertThat(result?.crn).isEqualTo("CRN1")
      assertThat(result?.createdByUsername).isEqualTo("test-client")
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-1.sql",
    )
    fun `Get licences by prisoner number is role protected`() {
      val result = webTestClient.get()
        .uri("/public/licence-summaries/prison-number/A1234AA")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  @Nested
  inner class `Get exclusion zone image by condition ID` {
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    @ParameterizedTest
    @CsvSource("ROLE_VIEW_LICENCES", "ROLE_SAR_DATA_ACCESS")
    fun `Get exclusion zone image by condition ID`(role: String) {
      // Given upload detail has UUID stored
      additionalConditionUploadDetailRepository.save(
        additionalConditionUploadDetailRepository.findById(1).orElseThrow()
          .copy(fullSizeImageDsUuid = "44f8163c-6c97-4ff2-932b-ae24feb0c112"),
      )
      // Given document service has the file uploaded to it
      documentApiMockServer.stubDownloadDocumentFile(
        withUUID = "44f8163c-6c97-4ff2-932b-ae24feb0c112",
        document = byteArrayOf(9, 9, 9),
      )

      val result = webTestClient.get()
        .uri("/public/licences/2/conditions/1/image-upload")
        .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.IMAGE_JPEG)
        .expectBody().returnResult()

      // Then I get back the image that was previously uploaded to the document service
      assertThat(result.responseBody).isEqualTo(byteArrayOf(9, 9, 9))
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Get exclusion zone image by condition ID is role-protected`() {
      val result = webTestClient.get()
        .uri("/public/licences/2/conditions/1/image-upload")
        .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  private companion object {
    val documentApiMockServer = DocumentApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      documentApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      documentApiMockServer.stop()
    }
  }
}
