package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DocumentApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleUploadAdditionalCondition
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
    @Sql("classpath:test_data/seed-licence-id-1.sql")
    fun `Get licences by CRN`() {
      // Given
      val request = buildGetRequest(uri = "/public/licence-summaries/crn/CRN1")

      // When
      val response = request.exchange()

      // Then
      val resultList = response
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(PublicLicenceSummary::class.java)
        .returnResult().responseBody

      assertThat(resultList).hasSize(1)
      assertLicenceSummary(resultList!!.first())
    }

    @Test
    @Sql("classpath:test_data/seed-licence-id-1.sql")
    fun `Get licences by CRN is role protected`() {
      // Given
      val request = buildGetRequest(uri = "/public/licence-summaries/crn/CRN1", role = "ROLE_CVL_VERY_WRONG")

      // When
      val response = request.exchange()

      // Then
      val result = response
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  @Nested
  inner class `Get licences by prisoner number` {

    @Test
    @Sql("classpath:test_data/seed-licence-id-1.sql")
    fun `Get licences by prisoner number`() {
      // Given
      val request = buildGetRequest(uri = "/public/licence-summaries/prison-number/A1234AA")

      // When
      val response = request.exchange()

      // Then
      val resultList = response
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBodyList(PublicLicenceSummary::class.java)
        .returnResult().responseBody

      assertThat(resultList).hasSize(1)
      val result = resultList!!.first()
      assertLicenceSummary(result)
    }

    @Test
    @Sql("classpath:test_data/seed-licence-id-1.sql")
    fun `Get licences by prisoner number is role protected`() {
      // Given
      val request = buildGetRequest(uri = "/public/licence-summaries/prison-number/A1234AA", role = "ROLE_CVL_VERY_WRONG")

      // When
      val response = request.exchange()

      // Then
      val result = response
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  @Deprecated(message = "The controller for this is deprecated")
  @Nested
  inner class `Get exclusion zone image by condition ID` {

    private val legacyUri = "/public/licences/2/conditions/1/image-upload"

    @ParameterizedTest
    @CsvSource("ROLE_VIEW_LICENCES", "ROLE_SAR_DATA_ACCESS")
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Get image for condition ID`(role: String) {
      // Given
      val uuid = "44f8163c-6c97-4ff2-932b-ae24feb0c112"
      val expectedContent = byteArrayOf(9, 9, 9)

      additionalConditionUploadDetailRepository.save(
        additionalConditionUploadDetailRepository.findById(1).orElseThrow()
          .copy(fullSizeImageDsUuid = uuid),
      )
      documentApiMockServer.stubDownloadDocumentFile(withUUID = uuid, document = expectedContent)
      val request = buildGetRequest(uri = legacyUri, role = role)

      // When
      val response = request.exchange()

      // Then
      val result = response
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.IMAGE_JPEG)
        .expectBody()
        .returnResult()

      assertThat(result.responseBody).isEqualTo(expectedContent)
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Get exclusion zone image by condition ID is role-protected`() {
      // Given
      val request = buildGetRequest(uri = legacyUri, role = "ROLE_CVL_VERY_WRONG")

      // When
      val response = request.exchange()

      // Then
      val result = response
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  @Nested
  inner class `Get supported document by condition ID` {

    @ParameterizedTest
    @CsvSource("ROLE_VIEW_LICENCES", "ROLE_SAR_DATA_ACCESS")
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Get supporting document for condition ID`(role: String) {
      // Given
      val uuid = "44f8163c-6c97-4ff2-932b-ae24feb0c112"
      val expectedContent = byteArrayOf(9, 9, 9)

      additionalConditionUploadDetailRepository.save(
        additionalConditionUploadDetailRepository.findById(1).orElseThrow()
          .copy(fullSizeImageDsUuid = uuid),
      )
      documentApiMockServer.stubDownloadDocumentFile(withUUID = uuid, document = expectedContent)
      val request = buildGetRequest(role = role)

      // When
      val response = request.exchange()

      // Then
      val result = response
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.IMAGE_JPEG)
        .expectBody()
        .returnResult()

      assertThat(result.responseBody).isEqualTo(expectedContent)
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Get supporting document by condition ID is role-protected`() {
      // Given
      val request = buildGetRequest(role = "ROLE_CVL_VERY_WRONG")

      // When
      val response = request.exchange()

      // Then
      val result = response
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }
  }

  @Nested
  inner class `Transform additional conditions` {

    @BeforeEach
    fun reset() {
      govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Transforms exclusion zone and restricted areas correctly`() {
      // Given
      val request = buildGetRequest(
        uri = "/public/licences/id/2",
        role = "ROLE_VIEW_LICENCES",
        acceptMediaTypes = listOf(MediaType.APPLICATION_JSON),
      )

      // When
      val response = request.exchange()

      // Then
      val licence = response
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(Licence::class.java)
        .returnResult().responseBody!!

      val additionalConditions = licence.conditions.apConditions.additional
      assertThat(additionalConditions).hasSize(2)
      assertThat(additionalConditions[0]).isInstanceOf(MultipleExclusionZoneAdditionalCondition::class.java)
      assertThat(additionalConditions[1]).isInstanceOf(MultipleUploadAdditionalCondition::class.java)
      assertUploadCondition(additionalConditions[0], "MULTIPLE_EXCLUSION_ZONE", setOf("MULTIPLE_EXCLUSION_ZONE"), 1, "Freedom of movement", "0f9a20f4-35c7-4c77-8af8-f200f153fa11", "c1t", true)
      assertUploadCondition(additionalConditions[1], "MULTIPLE_UPLOADS", setOf("MULTIPLE_UPLOADS"), 2, "restricted area", "005d70e4-a247-4f82-b8b3-6d294a0f5051", "c2t", true)
    }

    fun assertUploadCondition(
      actual: AdditionalCondition,
      expectedType: String,
      allowedTypes: Set<String>,
      expectedId: Long,
      expectedCategory: String,
      expectedCode: String,
      expectedText: String,
      expectedHasImageUpload: Boolean,
    ) {
      assertThat(allowedTypes).contains(expectedType)
      assertThat(actual.type).isEqualTo(expectedType)
      assertThat(actual.id).isEqualTo(expectedId)
      assertThat(actual.category).isEqualTo(expectedCategory)
      assertThat(actual.code).isEqualTo(expectedCode)
      assertThat(actual.text).isEqualTo(expectedText)

      val hasImageUpload = when (actual) {
        is MultipleExclusionZoneAdditionalCondition -> actual.hasImageUpload
        is MultipleUploadAdditionalCondition -> actual.hasImageUpload
        else -> null
      }
      hasImageUpload?.let { assertThat(it).isEqualTo(expectedHasImageUpload) }
    }
  }

  private fun assertLicenceSummary(result: PublicLicenceSummary, id: Long = 1L, crn: String = "CRN1", prisonNumber: String = "A1234AA") {
    assertThat(result.id).isEqualTo(id)
    assertThat(result.kind).isEqualTo(LicenceKind.CRD)
    assertThat(result.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result.policyVersion).isEqualTo(PolicyVersion.V1_0)
    assertThat(result.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(result.prisonNumber).isEqualTo(prisonNumber)
    assertThat(result.bookingId).isEqualTo(12345L)
    assertThat(result.crn).isEqualTo(crn)
    assertThat(result.createdByUsername).isEqualTo("test-client")
    assertThat(result.version).isEqualTo("1.0")
  }

  private fun buildGetRequest(
    uri: String = "/public/licence/2/condition/1/supporting-document",
    role: String = "ROLE_VIEW_LICENCES",
    acceptMediaTypes: List<MediaType> = listOf(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON),
  ): WebTestClient.RequestHeadersSpec<*> = webTestClient.get()
    .uri(uri)
    .accept(*acceptMediaTypes.toTypedArray())
    .headers(setAuthorisation(roles = listOf(role)))

  private companion object {
    val documentApiMockServer = DocumentApiMockServer()
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      documentApiMockServer.start()
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      documentApiMockServer.stop()
      govUkApiMockServer.stop()
    }
  }
}
