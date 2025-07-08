package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdatePrisonInformationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateReasonForVariationRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSpoDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateVloDiscussionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence as EntityVariationLicence

class LicenceIntegrationTest : IntegrationTestBase() {
  @MockitoBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  lateinit var licenceRepository: LicenceRepository

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
      .expectBody(Licence::class.java)
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
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.electronicMonitoringProviderStatus).isEqualTo(ElectronicMonitoringProviderStatus.NOT_STARTED)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-4.sql",
  )
  fun `should return ElectronicMonitoringProviderStatus as NOT_NEEDED`() {
    val result = webTestClient.get()
      .uri("/licence/id/3")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
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
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
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
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Approved licence`() {
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
      .expectBody(Licence::class.java)
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
    val licence = licenceRepository.findById(1).getOrNull()
    assertThat(licence).isNotNull
    assertThat(licence!!).isInstanceOf(PrrdLicence::class.java)
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.APPROVED)
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
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(licenceV1?.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licenceV1?.updatedByUsername).isEqualTo(aStatusToApprovedUpdateRequest.username)

    val licenceV2 = webTestClient.get()
      .uri("/licence/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(licenceV2?.statusCode).isEqualTo(LicenceStatus.APPROVED)
    assertThat(licenceV2?.approvedByUsername).isEqualTo(aStatusToApprovedUpdateRequest.username)
    assertThat(licenceV2?.approvedByName).isEqualTo(aStatusToApprovedUpdateRequest.fullName)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Submit licence`() {
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    webTestClient.put()
      .uri("/licence/id/1/submit")
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
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(LicenceStatus.SUBMITTED)
    assertThat(result?.comUsername).isEqualTo("test-client")
    assertThat(result?.comEmail).isEqualTo("testClient@probation.gov.uk")
    assertThat(result?.comStaffId).isEqualTo(2000)
    assertThat(result?.updatedByUsername).isEqualTo("test-client")
    assertThat(result?.submittedByFullName).isEqualTo("Test Client")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prrd-licence-id-1.sql",
  )
  fun `Submit PRRD licence`() {
    // Given
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    webTestClient.put()
      .uri("/licence/id/1/submit")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    // Then
    val licence = licenceRepository.findById(1).getOrNull()
    assertThat(licence).isNotNull
    assertThat(licence!!).isInstanceOf(PrrdLicence::class.java)
    assertThat(licence.kind).isEqualTo(LicenceKind.PRRD)
    assertThat(licence.postRecallReleaseDate).isNotNull()
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.SUBMITTED)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Create licence variation`() {
    val result = webTestClient.post()
      .uri("/licence/id/1/create-variation")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(result?.licenceId).isGreaterThan(1)
    assertThat(result?.licenceType).isEqualTo(LicenceType.AP)
    assertThat(result?.licenceStatus).isEqualTo(LicenceStatus.VARIATION_IN_PROGRESS)
    assertThat(licenceRepository.count()).isEqualTo(2)

    val newLicence = webTestClient.get()
      .uri("/licence/id/${result?.licenceId}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(VariationLicence::class.java)
      .returnResult().responseBody

    assertThat(newLicence?.variationOf).isEqualTo(1)
    assertThat(newLicence?.licenceVersion).isEqualTo("2.0")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-prrd-licence-id-1.sql",
  )
  fun `Create PRRD licence variation`() {
    // Given
    val uri = "/licence/id/1/create-variation"
    val roles = listOf("ROLE_CVL_ADMIN")

    // When
    val result = post(uri, roles)

    // Then
    result.expectStatus().isOk

    val licence = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(licence).isNotNull
    assertThat(licence!!.kind).isEqualTo(LicenceKind.VARIATION)

    val persistedLicence = licenceRepository.findById(licence.licenceId).getOrNull()
    assertThat(persistedLicence).isNotNull
    assertThat(persistedLicence).isInstanceOf(EntityVariationLicence::class.java)
    val persistedVariationLicence = persistedLicence as EntityVariationLicence
    assertThat(persistedVariationLicence.id).isEqualTo(2)
    assertThat(persistedVariationLicence.variationOfId).isEqualTo(1)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-approved-licence-1.sql",
  )
  fun `Edit an approved licence`() {
    // Given
    val uri = "/licence/id/1/edit"
    val roles = listOf("ROLE_CVL_ADMIN")
    prisonerSearchApiMockServer.stubSearchPrisonersByNomisIds()

    // When
    val result = post(uri, roles)

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
    val result = post(uri, roles)

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
    val result = post(uri, roles)

    // Then
    assertEdit(result, LicenceKind.HDC)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `Discard licence`() {
    webTestClient.delete()
      .uri("/licence/id/1/discard")
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(licenceRepository.count()).isEqualTo(0)
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
      .expectBody(VariationLicence::class.java)
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
      .expectBody(VariationLicence::class.java)
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
  fun `Update prison information`() {
    webTestClient.put()
      .uri("/licence/id/1/prison-information")
      .bodyValue(
        UpdatePrisonInformationRequest(
          prisonCode = "PVI",
          prisonDescription = "Pentonville (HMP)",
          prisonTelephone = "+44 276 54545",
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
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.prisonCode).isEqualTo("PVI")
    assertThat(result?.prisonDescription).isEqualTo("Pentonville (HMP)")
    assertThat(result?.prisonTelephone).isEqualTo("+44 276 54545")
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
      .expectBody(Licence::class.java)
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
      .expectBody(Licence::class.java)
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
      .expectBody(Licence::class.java)
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
      .expectBody(Licence::class.java)
      .returnResult().responseBody

    assertThat(result?.statusCode).isEqualTo(aStatusToInactiveUpdateRequest.status)
    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher).publishDomainEvent(capture())
      assertThat(firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value)
    }
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
        val licence = licenceRepository.findById(1L).get() as HardStopLicence
        assertThat(licence.reviewDate).isNull()

        val result = webTestClient.get()
          .uri("/licence/id/1")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody(Licence::class.java)
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
        val licence = licenceRepository.findById(1L).get() as HardStopLicence
        assertThat(licence.reviewDate?.toLocalDate()).isEqualTo(LocalDate.now())

        val result = webTestClient.get()
          .uri("/licence/id/1")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody(Licence::class.java)
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
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
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
        .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
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

        val variation = licenceRepository.findById(2L).get() as EntityVariationLicence
        assertThat(variation.statusCode).isEqualTo(LicenceStatus.VARIATION_APPROVED)

        val original = licenceRepository.findById(1L).get() as CrdLicence
        assertThat(original.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
      }

      webTestClient.put()
        .uri("/licence/id/2/activate-variation")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk

      run {
        val variation = licenceRepository.findById(2L).get() as EntityVariationLicence
        assertThat(variation.statusCode).isEqualTo(LicenceStatus.ACTIVE)

        val original = licenceRepository.findById(1L).get() as CrdLicence
        assertThat(original.statusCode).isEqualTo(LicenceStatus.INACTIVE)
      }
    }

    @Test
    fun `Get forbidden (403) when incorrect roles are supplied`() {
      val result = webTestClient.put()
        .uri("/licence/id/1/activate-variation")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_VERY_WRONG")))
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
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
        .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
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
        .expectBody(Licence::class.java)
        .returnResult().responseBody

      assertThat(result?.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
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

      assertThat(licenceRepository.count()).isEqualTo(1)

      val licence = webTestClient.get()
        .uri("/licence/id/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody(Licence::class.java)
        .returnResult().responseBody

      assertThat(licence?.statusCode).isEqualTo(LicenceStatus.APPROVED)
      assertThat(licence?.licenceVersion).isEqualTo("1.0")
    }
  }

  private fun assertEdit(result: WebTestClient.ResponseSpec, expectedKind: LicenceKind) {
    result.expectStatus().isOk
    val licenceSummary = result.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LicenceSummary::class.java)
      .returnResult().responseBody

    assertThat(licenceSummary!!.licenceId).isGreaterThan(1)
    assertThat(licenceSummary.licenceType).isEqualTo(LicenceType.AP)
    assertThat(licenceSummary.licenceStatus).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licenceRepository.count()).isEqualTo(2)

    val newLicence = licenceRepository.findById(licenceSummary.licenceId).getOrNull()
    assertThat(newLicence).isNotNull
    assertThat(newLicence!!.kind).isEqualTo(expectedKind)
    assertThat(newLicence.licenceVersion).isEqualTo("1.1")

    val versionOfId = when (newLicence) {
      is CrdLicence -> newLicence.versionOfId
      is HdcLicence -> newLicence.versionOfId
      is PrrdLicence -> newLicence.versionOfId
      else -> null
    }

    assertThat(versionOfId).isEqualTo(1)
  }

  private fun post(
    uri: String,
    roles: List<String>,
  ): WebTestClient.ResponseSpec {
    val result = webTestClient.post()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = roles))
      .exchange()
    return result
  }

  private companion object {
    val aStatusToApprovedUpdateRequest =
      StatusUpdateRequest(status = LicenceStatus.APPROVED, username = "AAA", fullName = "Y")
    val aStatusToActiveUpdateRequest =
      StatusUpdateRequest(status = LicenceStatus.ACTIVE, username = "AAA", fullName = "Y")
    val aStatusToInactiveUpdateRequest =
      StatusUpdateRequest(status = LicenceStatus.INACTIVE, username = "AAA", fullName = "Y")

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

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      prisonerSearchApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      prisonerSearchApiMockServer.stop()
    }
  }
}
