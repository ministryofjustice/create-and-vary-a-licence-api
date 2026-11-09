package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.DocumentApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.PublicLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.MultipleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.SingleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EVENT_EXCLUSION_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.EXCLUSION_ZONE_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.MULTIPLE_UPLOAD_COND_CODE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.PolicyVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class PublicLicenceServiceIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Autowired
  lateinit var additionalConditionUploadDetailRepository: AdditionalConditionUploadRepository

  @Nested
  inner class `Get HDC licence by id` {

    @Test
    @Sql(
      "classpath:test_data/seed-hdc-licence-id-1.sql",
      "classpath:test_data/seed-hdc-curfew-hours.sql",
      "classpath:test_data/seed-hdc-curfew-address.sql",
    )
    fun `returns HDC curfew address and times`() {
      assertHdcCurfewDetails("HDC")
    }

    @Test
    @Sql("classpath:test_data/seed-hdc-licence-id-1-with-curfew-details.sql")
    fun `HDC licence response can be deserialized as a regular licence`() {
      // Given
      val request = buildGetRequest(uri = "/public/v2/licences/id/1")

      // When
      val response = request.exchange()

      // Then
      response.expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().json(serializedContent("hdc-licence"), JsonCompareMode.STRICT)
        .returnResult()
    }

    @Test
    @Sql(
      "classpath:test_data/seed-hdc-variation-licence-id-1.sql",
      "classpath:test_data/seed-hdc-curfew-hours.sql",
      "classpath:test_data/seed-hdc-curfew-address.sql",
    )
    fun `returns HDC variation curfew address and times`() {
      assertHdcCurfewDetails("HDC_VARIATION")
    }

    private fun assertHdcCurfewDetails(kind: String) {
      // Given
      val request = buildGetRequest(uri = "/public/v2/licences/id/1")

      // When
      val response = request.exchange()

      // Then
      response
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.id").isEqualTo(1)
        .jsonPath("$.kind").isEqualTo(kind)
        .jsonPath("$.curfewAddress.firstLine").isEqualTo("1 Some Address")
        .jsonPath("$.curfewAddress.townOrCity").isEqualTo("Some Town")
        .jsonPath("$.curfewAddress.postcode").isEqualTo("AB1 2CD")
        .jsonPath("$.firstNightCurfewTimes.fromTime").isEqualTo("18:00:00")
        .jsonPath("$.firstNightCurfewTimes.untilTime").isEqualTo("08:00:00")
        .jsonPath("$.curfewTimes[0].fromDay").isEqualTo("MONDAY")
        .jsonPath("$.curfewTimes[0].fromTime").isEqualTo("19:00:00")
        .jsonPath("$.curfewTimes[1].fromDay").isEqualTo("TUESDAY")

      buildGetRequest(uri = "/public/licences/id/1").exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.kind").isEqualTo(kind)
        .jsonPath("$.curfewAddress").doesNotExist()
        .jsonPath("$.firstNightCurfewTimes").doesNotExist()
        .jsonPath("$.curfewTimes").doesNotExist()
    }
  }

  @Nested
  inner class `Get CRD licence by id` {

    @ParameterizedTest
    @ValueSource(strings = ["/public", "/public/v2"])
    @Sql("classpath:test_data/seed-crd-licence-id-1-with-conditions.sql")
    fun `CRD licence response can be deserialized as a regular licence`(apiPath: String) {
      // Given
      val request = buildGetRequest(uri = "$apiPath/licences/id/1")

      // When
      val response = request.exchange()

      // Then
      response.expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().json(serializedContent("crd-licence"), JsonCompareMode.STRICT)
        .returnResult()
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["/public", "/public/v2"])
  fun `get licence by id requires authentication`(apiPath: String) {
    webTestClient.get().uri("$apiPath/licences/id/1").exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["/public", "/public/v2"])
  fun `get licence by id requires view licences role`(apiPath: String) {
    buildGetRequest(uri = "$apiPath/licences/id/1", role = "ROLE_CVL_VERY_WRONG").exchange()
      .expectStatus().isForbidden
  }

  @ParameterizedTest
  @ValueSource(strings = ["/public", "/public/v2"])
  fun `get licence by id returns not found for missing licences`(apiPath: String) {
    buildGetRequest(uri = "$apiPath/licences/id/99999").exchange()
      .expectStatus().isNotFound
  }

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
      assertLicenceSummary(resultList.first())
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
      val result = resultList.first()
      assertLicenceSummary(result)
    }

    @Test
    @Sql("classpath:test_data/seed-licence-id-1.sql")
    fun `Get licences by prisoner number is role protected`() {
      // Given
      val request =
        buildGetRequest(uri = "/public/licence-summaries/prison-number/A1234AA", role = "ROLE_CVL_VERY_WRONG")

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
  inner class `Transform additional conditions` {

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
      assertUploadCondition(
        additionalConditions[0],
        "MULTIPLE_EXCLUSION_ZONE",
        setOf("MULTIPLE_EXCLUSION_ZONE"),
        1,
        "Freedom of movement",
        EXCLUSION_ZONE_COND_CODE,
        "c1t",
        true,
        "Description",
      )
      assertUploadCondition(
        additionalConditions[1],
        "MULTIPLE_UPLOADS",
        setOf("MULTIPLE_UPLOADS"),
        2,
        "restricted area",
        MULTIPLE_UPLOAD_COND_CODE,
        "c2t",
        true,
        "Description",
      )
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
      "classpath:test_data/add-event-exclusion-to-licence-id-2.sql",
    )
    fun `Transforms event exclusion with upload description`() {
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

      val eventExclusion = licence.conditions.apConditions.additional
        .single { it.code == EVENT_EXCLUSION_COND_CODE }
      assertThat(eventExclusion).isInstanceOf(SingleUploadAdditionalCondition::class.java)
      assertUploadCondition(
        eventExclusion,
        "SINGLE_UPLOAD",
        setOf("SINGLE_UPLOAD"),
        eventExclusion.id,
        "Freedom of movement",
        EVENT_EXCLUSION_COND_CODE,
        "c3t",
        true,
        "Event exclusion zone",
      )
    }

    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-2.sql",
      "classpath:test_data/add-upload-to-licence-id-2.sql",
    )
    fun `Returns null description when upload description is null`() {
      // Given
      additionalConditionUploadDetailRepository.findAll().forEach {
        additionalConditionUploadDetailRepository.save(it.copy(description = null))
      }
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
      assertUploadCondition(
        additionalConditions[0],
        "MULTIPLE_EXCLUSION_ZONE",
        setOf("MULTIPLE_EXCLUSION_ZONE"),
        1,
        "Freedom of movement",
        "0f9a20f4-35c7-4c77-8af8-f200f153fa11",
        "c1t",
        true,
        null,
      )
      assertUploadCondition(
        additionalConditions[1],
        "MULTIPLE_UPLOADS",
        setOf("MULTIPLE_UPLOADS"),
        2,
        "restricted area",
        "005d70e4-a247-4f82-b8b3-6d294a0f5051",
        "c2t",
        true,
        null,
      )
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
      expectedDescription: String?,
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
        is SingleUploadAdditionalCondition -> actual.hasImageUpload
        else -> null
      }
      hasImageUpload?.let { assertThat(it).isEqualTo(expectedHasImageUpload) }

      val description = when (actual) {
        is MultipleExclusionZoneAdditionalCondition -> actual.description
        is MultipleUploadAdditionalCondition -> actual.description
        is SingleUploadAdditionalCondition -> actual.description
        else -> null
      }
      assertThat(description).isEqualTo(expectedDescription)
    }
  }

  private fun assertLicenceSummary(
    result: PublicLicenceSummary,
    id: Long = 1L,
    crn: String = "CRN1",
    prisonNumber: String = "A1234AA",
  ) {
    assertThat(result.id).isEqualTo(id)
    assertThat(result.kind).isEqualTo(LicenceKind.CRD)
    assertThat(result.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result.policyVersion).isEqualTo(PolicyVersion.V1_0)
    assertThat(result.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(result.prisonNumber).isEqualTo(prisonNumber)
    assertThat(result.bookingId).isEqualTo(123L)
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

  private fun serializedContent(name: String) = this.javaClass.getResourceAsStream("/test_data/public-api/$name.json")!!
    .bufferedReader()
    .readText()

  private companion object {
    @RegisterExtension
    val documentApiMockServer = DocumentApiMockServer()
  }
}
