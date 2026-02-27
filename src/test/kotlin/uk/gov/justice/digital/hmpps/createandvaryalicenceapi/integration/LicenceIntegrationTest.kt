package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Variation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DocumentApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CreateVariationResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EditLicenceResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.LicencePermissionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LicencePermissionsResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.AP
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence as EntityVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence as LicenceDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VariationLicence as VariationLicenceDto

class LicenceIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @BeforeEach
  fun reset() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get a licence by ID`() {
    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    log.info("Expect OK: Licence is ${mapper.writeValueAsString(result)}")

    assertThat(result?.standardLicenceConditions?.size).isEqualTo(2)
    assertThat(result?.standardLicenceConditions)
      .extracting("code")
      .containsAll(listOf("goodBehaviour", "notBreakLaw"))
    assertThat(result?.standardPssConditions?.size).isEqualTo(1)
    assertThat(result?.standardPssConditions)
      .extracting("code")
      .containsAll(listOf("attendMeetings"))
    assertThat(result?.responsibleComFullName).isEqualTo("Test Client")
    assertThat(result?.electronicMonitoringProviderStatus).isEqualTo(ElectronicMonitoringProviderStatus.COMPLETE)
  }

  @Test
  @Sql("classpath:test_data/seed-licence-id-1.sql")
  fun `Get a licence by ID - new testing approach`() {
    // Given
    val requestSpec = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))

    // When
    val responseSpec = requestSpec.exchange()

    // Then
    val variables = mapOf(
      "licenceId" to 1,
      "responsibleComFullName" to "Test Client",
      "electronicMonitoringProviderStatus" to "COMPLETE",
    )

    jsonTestUtils.assertJsonEquals(
      templateName = "licence_by_id",
      variables = variables,
      responseSpec = responseSpec,
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-5.sql",
  )
  fun `should return ElectronicMonitoringProviderStatus as NotStarted`() {
    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.electronicMonitoringProviderStatus).isEqualTo(ElectronicMonitoringProviderStatus.NOT_STARTED)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-4.sql",
  )
  fun `should return ElectronicMonitoringProviderStatus as NOT_NEEDED`() {
    documentApiMockServer.stubDownloadDocumentFile(
      withUUID = "92939445-4159-4214-aa75-d07568a3e136",
      document = byteArrayOf(9, 9, 9),
    )

    val result = webTestClient.get()
      .uri("/licence/id/3")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.electronicMonitoringProviderStatus).isEqualTo(ElectronicMonitoringProviderStatus.NOT_NEEDED)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val result = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
      .exchange()
      .expectStatus().isEqualTo(FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access Denied")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(UNAUTHORIZED.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update the status of a licence to approved`() {
    webTestClient.put()
      .uri("/licence/id/1/status")
      .bodyValue(aStatusToApprovedUpdateRequest)
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
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(aStatusToApprovedUpdateRequest.status)
    assertThat(result?.updatedByUsername).isEqualTo(aStatusToApprovedUpdateRequest.username)
    assertThat(result?.approvedByUsername).isEqualTo(aStatusToApprovedUpdateRequest.username)
    assertThat(result?.approvedByName).isEqualTo(aStatusToApprovedUpdateRequest.fullName)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-submitted-prrd-licence-id-1.sql",
  )
  fun `Approved PRRD licence`() {
    // Given
    val uri = "/licence/id/1/status"
    val roles = listOf("ROLE_CVL_ADMIN")

    // When
    val result = webTestClient.put()
      .uri(uri)
      .bodyValue(aStatusToApprovedUpdateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = roles))
      .exchange()

    // Then
    result.expectStatus().isOk
    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(PrrdLicence::class.java)
    assertThat(licence.statusCode).isEqualTo(APPROVED)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-versioned-licence-id-1.sql",
  )
  fun `Approve a new version of a licence`() {
    webTestClient.put()
      .uri("/licence/id/2/status")
      .bodyValue(aStatusToApprovedUpdateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val licenceV1 = webTestClient.get()
      .uri("/licence/id/1")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(licenceV1?.statusCode).isEqualTo(INACTIVE)
    assertThat(licenceV1?.updatedByUsername).isEqualTo(aStatusToApprovedUpdateRequest.username)

    val licenceV2 = webTestClient.get()
      .uri("/licence/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(licenceV2?.statusCode).isEqualTo(APPROVED)
    assertThat(licenceV2?.approvedByUsername).isEqualTo(aStatusToApprovedUpdateRequest.username)
    assertThat(licenceV2?.approvedByName).isEqualTo(aStatusToApprovedUpdateRequest.fullName)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Submit Crd licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/submit")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(CrdLicence::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.CRD)

    licence as CrdLicence
    assertThat(licence.statusCode).isEqualTo(SUBMITTED)
    assertThat(licence.responsibleCom?.username).isEqualTo("test-client")
    assertThat(licence.responsibleCom?.email).isEqualTo("testClient@probation.gov.uk")
    assertThat(licence.responsibleCom?.staffIdentifier).isEqualTo(2000)
    assertThat(licence.updatedByUsername).isEqualTo("test-client")
    assertThat(licence.submittedBy?.fullName).isEqualTo("Test Client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prrd-licence-id-1.sql",
  )
  fun `Submit PRRD licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/submit")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk

    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(PrrdLicence::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.PRRD)
    assertThat(licence.postRecallReleaseDate).isNotNull()
    assertThat(licence.statusCode).isEqualTo(SUBMITTED)
  }

  @Test
  @SqlGroup(
    Sql("classpath:test_data/seed-prison-case-administrator.sql"),
    Sql("classpath:test_data/seed-completed-hard-stop-licence-1.sql"),
  )
  fun `Submit Hard Stop licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/submit")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "pca", roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(HardStopLicence::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.HARD_STOP)
    assertThat(licence.statusCode).isEqualTo(SUBMITTED)
  }

  @Test
  @SqlGroup(
    Sql("classpath:test_data/seed-completed-variation-licence-1.sql"),
  )
  fun `Submit VARIATION licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/submit") // use the correct ID for VARIATION
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(Variation::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.VARIATION)
    assertThat(licence.statusCode).isEqualTo(VARIATION_SUBMITTED)
  }

  @Test
  @SqlGroup(
    Sql("classpath:test_data/seed-completed-hdc-licence-1.sql"),
  )
  fun `Submit HDC licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/submit") // correct ID for HDC
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(HdcLicence::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.HDC)
    assertThat(licence.statusCode).isEqualTo(SUBMITTED)
  }

  @Test
  @SqlGroup(
    Sql("classpath:test_data/seed-completed-hdc-variation-licence-1.sql"),
  )
  fun `Submit HDC_VARIATION licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = webTestClient.put()
      .uri("/licence/id/1/submit") // correct ID for HDC_VARIATION
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()

    // Then
    result.expectStatus().isOk
    val licence = testRepository.findLicence(1)
    assertThat(licence).isInstanceOf(HdcVariationLicence::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.HDC_VARIATION)
    assertThat(licence.statusCode).isEqualTo(VARIATION_SUBMITTED)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1-with-address.sql",
  )
  fun `Create licence variation`() {
    // Given
    val uri = "/licence/id/1/create-variation"

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    val response = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CreateVariationResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response!!.licenceId).isGreaterThan(1)

    assertThat(testRepository.countLicence()).isEqualTo(2)
    val oldLicence = testRepository.findLicence(1)
    val newLicence = testRepository.findLicence(2)

    assertThat(newLicence.licenceVersion).isEqualTo("2.0")
    assertThat(newLicence.appointment?.addressText).isEqualTo("123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA")

    assertThat(newLicence).isInstanceOf(EntityVariationLicence::class.java)
    assertThat((newLicence as EntityVariationLicence).variationOfId).isEqualTo(1)
    assertLicenceHasExpectedAddress(newLicence, newAddress = true)
    assertThat(newLicence.variationOfId).isEqualTo(1)
    assertThat(newLicence.licenceVersion).isEqualTo("2.0")

    assertThat(newLicence.standardConditions.size).isEqualTo(oldLicence.standardConditions.size)
    assertThat(newLicence.additionalConditions.size).isEqualTo(oldLicence.additionalConditions.size)
    assertThat(newLicence.bespokeConditions.size).isEqualTo(oldLicence.bespokeConditions.size)

    assertNoOverlap(newLicence.standardConditions, oldLicence.standardConditions) { it.id }
    assertNoOverlap(newLicence.bespokeConditions, oldLicence.bespokeConditions) { it.id }

    assertNoOverlap(newLicence.standardConditions, oldLicence.standardConditions) { it.licence.id }
    assertNoOverlap(newLicence.bespokeConditions, oldLicence.bespokeConditions) { it.licence.id }

    val doNotContainSameValeCallbacks: List<(AdditionalCondition) -> Any?> = listOf(
      { it.id },
      { it.licence.id },
      { it.additionalConditionData.firstOrNull()?.id },
      { it.additionalConditionData.firstOrNull()?.additionalCondition?.id },
      { it.additionalConditionUpload.firstOrNull()?.id },
      { it.additionalConditionUpload.firstOrNull()?.additionalCondition?.id },
    )
    assertNoOverlaps(doNotContainSameValeCallbacks, newLicence.additionalConditions, oldLicence.additionalConditions)

    val uploadOld = oldLicence.additionalConditions.first().additionalConditionUpload.first()
    val uploadNew = newLicence.additionalConditions.first().additionalConditionUpload.first()

    assertListsEqual(
      listOf(uploadNew),
      listOf(uploadOld),
      fieldsToIgnore = listOf("id", "additionalConditionId", "licence"),
    )
    assertListsNotEqual(listOf(uploadNew), listOf(uploadOld), listOf("originalData", "fullSizeImage"))

    assertListsEqual(newLicence.standardConditions, oldLicence.standardConditions)
    assertListsEqual(newLicence.additionalConditions, oldLicence.additionalConditions)
    assertListsEqual(newLicence.bespokeConditions, oldLicence.bespokeConditions)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence-id-1-inPssPeriod.sql",
  )
  fun `Create licence variation when in pss period then exclude bespoke conditions`() {
    // Given
    val uri = "/licence/id/1/create-variation"

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    val response = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CreateVariationResponse::class.java)
      .returnResult().responseBody

    val bespokeConditions = testRepository.getBespokeConditions(response!!.licenceId, assertNotEmpty = false)
    assertThat(bespokeConditions.size).isEqualTo(0)
  }

  private fun <T> assertNoOverlaps(
    extractors: List<(T) -> Any?>,
    newList: List<T>,
    oldList: List<T>,
  ) {
    extractors.forEach {
      assertNoOverlap(newList, oldList, it)
    }
  }

  private fun <T> assertNoOverlap(newList: List<T>, oldList: List<T>, selectorCallBack: (T) -> Any?) {
    assertThat(newList.map(selectorCallBack)).doesNotContainAnyElementsOf(oldList.map(selectorCallBack))
  }

  private fun <T> assertListsEqual(
    newList: List<T>,
    oldList: List<T>,
    fieldsToIgnore: List<String> = listOf("id", "createdAt", "updatedAt", "licence", "uploadDetailId"),
    nestedFieldsRegx: List<String> = fieldsToIgnore.map { ".*\\.$it" },
  ) {
    assertThat(newList)
      .usingRecursiveComparison()
      .ignoringFields(*fieldsToIgnore.toTypedArray())
      .ignoringFieldsMatchingRegexes(*nestedFieldsRegx.toTypedArray())
      .isEqualTo(oldList)
  }

  private fun <T> assertListsNotEqual(
    newList: List<T>,
    oldList: List<T>,
    fieldsToIgnore: List<String> = listOf("id", "createdAt", "updatedAt", "licence", "uploadDetailId"),
    nestedFieldsRegx: List<String> = fieldsToIgnore.map { ".*\\.$it" },
  ) {
    assertThat(newList)
      .usingRecursiveComparison()
      .ignoringFields(*fieldsToIgnore.toTypedArray())
      .ignoringFieldsMatchingRegexes(*nestedFieldsRegx.toTypedArray())
      .isNotEqualTo(oldList)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1-with-address.sql",
  )
  fun `Create licence HDC variation`() {
    // Given
    val uri = "/licence/id/1/create-variation"

    // When
    val result = postRequest(uri)

    // Then
    result.expectStatus().isOk

    val licenceSummary = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CreateVariationResponse::class.java)
      .returnResult().responseBody

    assertThat(licenceSummary).isNotNull
    assertThat(licenceSummary!!.licenceId).isGreaterThan(1)

    assertThat(testRepository.countLicence()).isEqualTo(2)

    val licence = testRepository.findLicence(licenceSummary.licenceId)

    with(licence as HdcVariationLicence) {
      assertThat(licenceVersion).isEqualTo("2.0")
      assertThat(typeCode).isEqualTo(AP)
      assertThat(statusCode).isEqualTo(VARIATION_IN_PROGRESS)
      assertThat(appointment?.addressText).isEqualTo("123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA")
      assertThat(variationOfId).isEqualTo(1)
      assertLicenceHasExpectedAddress(this)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-prrd-licence-id-1.sql",
  )
  fun `Create PRRD licence variation`() {
    // Given
    val uri = "/licence/id/1/create-variation"
    val roles = listOf("ROLE_CVL_ADMIN")
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = postRequest(uri, roles)

    // Then
    result.expectStatus().isOk

    val response = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(CreateVariationResponse::class.java)
      .returnResult().responseBody

    assertThat(response).isNotNull
    assertThat(response!!.licenceId).isGreaterThan(0)

    val persistedLicence = testRepository.findLicence(response.licenceId)
    assertThat(persistedLicence).isInstanceOf(EntityVariationLicence::class.java)
    val persistedVariationLicence = persistedLicence as EntityVariationLicence
    assertThat(persistedVariationLicence.id).isEqualTo(2)
    assertThat(persistedVariationLicence.variationOfId).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-licence-1-with-address.sql",
  )
  fun `Edit an approved licence`() {
    // Given
    val uri = "/licence/id/1/edit"
    val roles = listOf("ROLE_CVL_ADMIN")
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = postRequest(uri, roles)

    // Then
    assertEdit(result, LicenceKind.CRD)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-prrd-licence-id-1.sql",
  )
  fun `Edit an approved PRRD licence`() {
    // Given
    val uri = "/licence/id/1/edit"
    val roles = listOf("ROLE_CVL_ADMIN")
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = postRequest(uri, roles)

    // Then
    assertEdit(result, LicenceKind.PRRD)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-hdc-licence-id-1.sql",
  )
  fun `Edit an approved HDC licence`() {
    // Given
    val uri = "/licence/id/1/edit"
    val roles = listOf("ROLE_CVL_ADMIN")
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = postRequest(uri, roles)

    // Then
    assertEdit(result, LicenceKind.HDC)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-a-few-licences.sql",
    "classpath:test_data/seed-uploads-for-copied-licences.sql",
  )
  fun `Discard licence`() {
    documentApiMockServer.stubDeleteDocuments()

    webTestClient.delete()
      .uri("/licence/id/2/discard")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(testRepository.doesLicenceExist(2)).isFalse()

    // 3 set up in the above sql , 2 associated with licence 2
    assertThat(testRepository.findAllUploadSummary()).hasSize(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence.sql",
  )
  fun `Update spo discussion`() {
    webTestClient.put()
      .uri("/licence/id/2/spo-discussion")
      .bodyValue(UpdateSpoDiscussionRequest(spoDiscussion = "Yes"))
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
      .expectBody(VariationLicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.spoDiscussion).isEqualTo("Yes")
    assertThat(result?.id).isEqualTo(2)
    assertThat(result?.variationOf).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence.sql",
  )
  fun `Update vlo discussion`() {
    webTestClient.put()
      .uri("/licence/id/2/vlo-discussion")
      .bodyValue(UpdateVloDiscussionRequest(vloDiscussion = "Not applicable"))
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
      .expectBody(VariationLicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.vloDiscussion).isEqualTo("Not applicable")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-variation-licence.sql",
  )
  fun `Update reason for variation`() {
    webTestClient.put()
      .uri("/licence/id/2/reason-for-variation")
      .bodyValue(UpdateReasonForVariationRequest(reasonForVariation = "reason"))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val result = webTestClient.get()
      .uri("/events/match?licenceId=2&eventType=VARIATION_SUBMITTED_REASON")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(LicenceEvent::class.java)
      .returnResult().responseBody

    assertThat(result).isNotNull
    assertThat(result).hasSize(1)
    assertThat(result!!.first().eventDescription).isEqualTo("reason")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Update prison information with large prison telephone number`() {
    webTestClient.put()
      .uri("/licence/id/1/prison-information")
      .bodyValue(
        UpdatePrisonInformationRequest(
          prisonCode = "PVI",
          prisonDescription = "Pentonville (HMP)",
          prisonTelephone = "+44 20 7946 0958 #98765",
        ),
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
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.prisonCode).isEqualTo("PVI")
    assertThat(result?.prisonDescription).isEqualTo("Pentonville (HMP)")
    assertThat(result?.prisonTelephone).isEqualTo("+44 20 7946 0958 #98765")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-licence-1.sql",
  )
  fun `Update the status of approved licence to active and record licence activated event`() {
    webTestClient.put()
      .uri("/licence/id/1/status")
      .bodyValue(aStatusToActiveUpdateRequest)
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
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(aStatusToActiveUpdateRequest.status)

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher).publishDomainEvent(capture())
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_ACTIVATED.value)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-3.sql",
  )
  fun `Update the status of active licence to inactive and record licence inactivated event`() {
    webTestClient.put()
      .uri("/licence/id/3/status")
      .bodyValue(aStatusToInactiveUpdateRequest)
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
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(aStatusToInactiveUpdateRequest.status)
    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher).publishDomainEvent(capture())
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-variation-licence-id-1.sql",
  )
  fun `Update the status of approved variation licence to an active variation and record licence activated event`() {
    webTestClient.put()
      .uri("/licence/id/1/status")
      .bodyValue(aStatusToActiveUpdateRequest)
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
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(aStatusToActiveUpdateRequest.status)
    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher).publishDomainEvent(capture())
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-active-variation-licence-id-1.sql",
  )
  fun `Update the status of active variation licence to an inactive variation and record licence inactivated event`() {
    webTestClient.put()
      .uri("/licence/id/1/status")
      .bodyValue(aStatusToInactiveUpdateRequest)
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
      .expectBody(LicenceDto::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(aStatusToInactiveUpdateRequest.status)
    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher).publishDomainEvent(capture())
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value)
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-a-licence-with-a-variation.sql",
  )
  fun `Should check if a COM has permission to view a licence`() {
    deliusMockServer.stubGetOffenderManager()

    val result = webTestClient.post()
      .uri("/licence/id/1/permissions")
      .bodyValue(LicencePermissionsRequest(teamCodes = listOf("team-code-1")))
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody(LicencePermissionsResponse::class.java)
      .returnResult().responseBody
    assertThat(result?.view).isEqualTo(true)
  }

  @Nested
  inner class CheckReviewingLicences {
    @Test
    @Sql(
      "classpath:test_data/seed-prison-case-administrator.sql",
      "classpath:test_data/seed-hard-stop-licences.sql",
    )
    fun `Review licence successfully`() {
      run {
        val licence = testRepository.findLicence(1L) as HardStopLicence
        assertThat(licence.reviewDate).isNull()

        val result = webTestClient.get()
          .uri("/licence/id/1")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody(LicenceDto::class.java)
          .returnResult().responseBody

        assertThat(result!!.isReviewNeeded).isTrue
      }

      webTestClient.post()
        .uri("/licence/id/1/review-with-no-variation-required")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk

      run {
        val licence = testRepository.findLicence(1L) as HardStopLicence
        assertThat(licence.reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())

        val result = webTestClient.get()
          .uri("/licence/id/1")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody(LicenceDto::class.java)
          .returnResult().responseBody

        assertThat(result!!.isReviewNeeded).isFalse
      }
    }

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.post()
        .uri("/licence/id/1/review-with-no-variation-required")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.post()
        .uri("/licence/id/1/review-with-no-variation-required")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }
  }

  @Nested
  inner class CheckActivatingVariations {
    @Test
    @Sql(
      "classpath:test_data/seed-variation-licence.sql",
    )
    fun `Activate licence successfully`() {
      run {
        webTestClient.put()
          .uri("/licence/id/2/approve-variation")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
          .exchange()
          .expectStatus().isOk

        val variation = testRepository.findLicence(2L) as EntityVariationLicence
        assertThat(variation.statusCode).isEqualTo(VARIATION_APPROVED)

        val original = testRepository.findLicence(1L) as CrdLicence
        assertThat(original.statusCode).isEqualTo(IN_PROGRESS)
      }

      webTestClient.put()
        .uri("/licence/id/2/activate-variation")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk

      run {
        val variation = testRepository.findLicence(2L) as EntityVariationLicence
        assertThat(variation.statusCode).isEqualTo(ACTIVE)

        val original = testRepository.findLicence(1L) as CrdLicence
        assertThat(original.statusCode).isEqualTo(INACTIVE)
      }
    }

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.put()
        .uri("/licence/id/1/activate-variation")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(FORBIDDEN.value())
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

      assertThat(result?.userMessage).contains("Access Denied")
    }

    @Test
    fun `Unauthorized (401) when no token is supplied`() {
      webTestClient.post()
        .uri("/licence/id/1/activate-variation")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(UNAUTHORIZED.value())
    }
  }

  @Nested
  inner class ThrowErrorIfIneligible {
    @Test
    @Sql(
      "classpath:test_data/seed-licence-id-1.sql",
    )
    fun `licence submission should be prevented`() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(aPrisonerMissingReleaseDate)

      webTestClient.put()
        .uri("/licence/id/1/submit")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().is4xxClientError

      val result = webTestClient.get()
        .uri("/licence/id/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(LicenceDto::class.java)
        .returnResult().responseBody

      assertThat(result?.statusCode).isEqualTo(IN_PROGRESS)
      assertThat(result?.comUsername).isEqualTo("test-client")
      assertThat(result?.comEmail).isEqualTo("testClient@probation.gov.uk")
      assertThat(result?.comStaffId).isEqualTo(2000)
    }

    @Test
    @Sql(
      "classpath:test_data/seed-approved-licence-1.sql",
    )
    fun `Editing an approved licence should be prevented`() {
      prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds(aPrisonerMissingReleaseDate)

      webTestClient.post()
        .uri("/licence/id/1/edit")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().is4xxClientError

      assertThat(testRepository.countLicence()).isEqualTo(1)

      val licence = webTestClient.get()
        .uri("/licence/id/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(LicenceDto::class.java)
        .returnResult().responseBody

      assertThat(licence?.statusCode).isEqualTo(APPROVED)
      assertThat(licence?.licenceVersion).isEqualTo("1.0")
    }
  }

  private fun assertEdit(result: WebTestClient.ResponseSpec, expectedKind: LicenceKind, noAddress: Boolean = false) {
    result.expectStatus().isOk
    val licenceSummary = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(EditLicenceResponse::class.java)
      .returnResult().responseBody

    assertThat(licenceSummary!!.licenceId).isGreaterThan(1)

    assertThat(testRepository.countLicence()).isEqualTo(2)

    val newLicence = testRepository.findLicence(licenceSummary.licenceId)
    assertThat(newLicence.kind).isEqualTo(expectedKind)
    assertThat(newLicence.licenceVersion).isEqualTo("1.1")
    assertThat(newLicence.typeCode).isEqualTo(AP)
    assertThat(newLicence.statusCode).isEqualTo(IN_PROGRESS)

    if (noAddress) {
      assertLicenceHasExpectedAddress(newLicence, newAddress = true)
      assertThat(newLicence.appointment?.addressText).isEqualTo("123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA,ENGLAND")
    }

    val versionOfId = when (newLicence) {
      is CrdLicence -> newLicence.versionOfId
      is HdcLicence -> newLicence.versionOfId
      is PrrdLicence -> newLicence.versionOfId
      else -> null
    }

    assertThat(versionOfId).isEqualTo(1)
  }

  private fun postRequest(
    uri: String,
    roles: List<String> = listOf("ROLE_CVL_ADMIN"),
  ): WebTestClient.ResponseSpec {
    val result = webTestClient.post()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = roles))
      .exchange()
    return result
  }

  fun assertLicenceHasExpectedAddress(
    licence: Licence,
    appointmentAddress: String = "123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA",
    reference: String = "REF-123456",
    firstLine: String = "123 Test Street",
    secondLine: String? = "Apt 4B",
    townOrCity: String = "Testville",
    county: String? = "Testshire",
    postcode: String = "TE5 7AA",
    source: AddressSource = AddressSource.MANUAL,
    newAddress: Boolean = true,
    uprn: String? = null,
  ) {
    assertThat(licence.appointment).isNotNull
    assertThat(licence.appointment!!.addressText).isEqualTo(appointmentAddress)
    val address = licence.appointment?.address
    assertThat(address).isNotNull
    address?.let {
      if (newAddress) {
        assertThat(it.reference).isNotNull()
        assertThat(it.reference).isNotEqualTo(reference)
      } else {
        assertThat(it.reference).isEqualTo(reference)
      }
      assertThat(it.firstLine).isEqualTo(firstLine)
      assertThat(it.secondLine).isEqualTo(secondLine)
      assertThat(it.townOrCity).isEqualTo(townOrCity)
      assertThat(it.county).isEqualTo(county)
      assertThat(it.postcode).isEqualTo(postcode)
      assertThat(it.source).isEqualTo(source)
      assertThat(it.uprn).isEqualTo(uprn)
    }
  }

  private companion object {
    val aStatusToApprovedUpdateRequest =
      StatusUpdateRequest(status = APPROVED, username = "AAA", fullName = "Y")
    val aStatusToActiveUpdateRequest =
      StatusUpdateRequest(status = ACTIVE, username = "AAA", fullName = "Y")
    val aStatusToInactiveUpdateRequest =
      StatusUpdateRequest(status = INACTIVE, username = "AAA", fullName = "Y")

    val aPrisonerMissingReleaseDate = """[{
      "prisonerNumber": "A1234AA",
      "bookingId": "123",
      "status": "ACTIVE",
      "mostSeriousOffence": "Robbery",
      "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
      "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
      "homeDetentionCurfewEligibilityDate": null,
      "releaseDate": null,
      "confirmedReleaseDate": null,
      "conditionalReleaseDate": null,
      "paroleEligibilityDate": null,
      "actualParoleDate" : null,
      "postRecallReleaseDate": null,
      "legalStatus": "SENTENCED",
      "indeterminateSentence": false,
      "recall": false,
      "prisonId": "ABC",
      "bookNumber": "12345A",
      "firstName": "Test1",
      "lastName": "Person1",
      "dateOfBirth": "1985-01-01"
    }]"""

    val govUkApiMockServer = GovUkMockServer()
    val prisonerSearchApiMockServer = PrisonerSearchMockServer()
    val deliusMockServer = DeliusMockServer()
    val documentApiMockServer = DocumentApiMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      prisonerSearchApiMockServer.start()
      deliusMockServer.start()
      documentApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      prisonerSearchApiMockServer.stop()
      deliusMockServer.stop()
      documentApiMockServer.stop()
    }
  }
}
