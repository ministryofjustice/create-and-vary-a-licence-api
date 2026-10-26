package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_REFUSED_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_REFUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_REFUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.SUPERSEDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["hdc.event.listener.enabled=true"])
class HdcEventsListenerIntegrationTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var hdcEventsListener: HdcEventsListener

  @MockitoSpyBean
  lateinit var hdcStatusChangedHandler: HdcStatusChangedHandler

  @MockitoSpyBean
  lateinit var eventsPublisher: OutboundEventsPublisher

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @Test
  fun `An HDC opt out event is processed`() {
    // Given
    val event = createHdcStatusChangedEvent(
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
    )

    val eventJson = mapper.writeValueAsString(event)

    // When
    sendMessage(eventJson, HdcCvlEventType.OPT_OUT.toString())

    // Then
    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handleOptOut(eventJson)
    assertThat(testRepository.findAllAuditEvents()).isEmpty()
    verifyNoInteractions(eventsPublisher)
  }

  @Test
  fun `An HDC event with unknown event type is sent to DLQ`() {
    // Given
    val event = createHdcStatusChangedEvent(
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      reason = "Some reason",
    )

    val eventJson = mapper.writeValueAsString(event)

    // When
    sendMessage(eventJson, "unknown-event-type")

    // Then
    assertSqsProcessed()
    verify(hdcStatusChangedHandler, times(0)).handleOptOut(eventJson)
    assertThat(testRepository.findAllAuditEvents()).isEmpty()
    verifyNoInteractions(eventsPublisher)
  }

  @SqlGroup(
    Sql("classpath:test_data/seed-licence-id-1.sql"),
    Sql("classpath:test_data/seed-hdc-conversion-details.sql"),
  )
  @ParameterizedTest
  @EnumSource(value = LicenceStatus::class, names = ["IN_PROGRESS", "SUBMITTED", "APPROVED"])
  fun `An HDC opt in event converts a single eligible CRD licence to an in progress HDC licence`(
    status: LicenceStatus,
  ) {
    // Given
    testRepository.updateLicenceStatus(1L, status)
    val event = createHdcStatusChangedEvent(
      bookingId = 123L,
      nomsNumber = "A1234AA",
      reason = "Offender opted in",
    )

    // When
    val eventJson = sendConversionEvent(event, HdcCvlEventType.OPT_IN)

    // Then
    verify(hdcStatusChangedHandler).handleOptIn(eventJson)
    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)

    val crdLicence = licences.single { it.kind == LicenceKind.CRD }
    assertThat(crdLicence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

    val hdcLicence = testRepository.findLicence(licences.single { it.kind == LicenceKind.HDC }.id) as HdcLicence
    assertThat(hdcLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(hdcLicence.versionOfId).isEqualTo(crdLicence.id)
    assertThat(hdcLicence.licenceVersion).isEqualTo("1.0")
    assertThat(hdcLicence.nomsId).isEqualTo(crdLicence.nomsId)
    assertThat(hdcLicence.bookingId).isEqualTo(crdLicence.bookingId)
    assertThat(hdcLicence.licenceStartDate).isEqualTo(crdLicence.licenceStartDate)
    assertThat(hdcLicence.probationContact).isNull()
    assertThat(hdcLicence.additionalConditions).hasSize(1)
    assertThat(hdcLicence.additionalConditions.single().expandedConditionText)
      .isEqualTo("Not to enter exclusion zone Town centre")
    assertThat(hdcLicence.additionalConditions.single().additionalConditionData.single().dataValue)
      .isEqualTo("Town centre")
    assertThat(hdcLicence.bespokeConditions).hasSize(1)
    assertThat(hdcLicence.bespokeConditions.single().conditionText).isEqualTo("Do not contact Person A")
    assertThat(hdcLicence.standardConditions).hasSize(8)

    webTestClient.get()
      .uri("/licence/id/${hdcLicence.id}")
      .headers(setAuthorisation(roles = cvlRoles()))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.isReplacementForOptedInLicence").isEqualTo(true)

    val audits = testRepository.findAllAuditEvents()
    assertThat(audits).hasSize(4)
    assertThat(audits)
      .filteredOn { it.licenceId == crdLicence.id }
      .extracting("summary")
      .containsExactlyInAnyOrder(
        "Licence automatically inactivated after HDC OPT_IN event for Person One",
        "CRD licence converted to HDC licence on OPT_IN",
      )
    assertThat(audits)
      .filteredOn { it.licenceId == hdcLicence.id }
      .extracting("summary")
      .containsExactlyInAnyOrder(
        "Updated standard conditions to policy version ${hdcLicence.version} for Person One",
        "HDC licence converted from CRD on OPT_IN",
      )

    val conversionDetail =
      "Old ID ${crdLicence.id}, new ID ${hdcLicence.id} type ${hdcLicence.typeCode} " +
        "status IN_PROGRESS version ${hdcLicence.version}"
    assertThat(audits.filter { it.summary?.contains("converted") == true })
      .hasSize(2)
      .allSatisfy { assertThat(it.detail).isEqualTo(conversionDetail) }

    assertThat(testRepository.findAllEventRepository())
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .containsExactly(
        tuple(
          crdLicence.id,
          SUPERSEDED,
          "SYSTEM",
          "SYSTEM",
          "SYSTEM",
          "Licence automatically inactivated after HDC OPT_IN event for Person One",
        ),
        tuple(
          crdLicence.id,
          INACTIVE_WHEN_HDC_OPT_IN,
          "SYSTEM_USER",
          "SYSTEM",
          "SYSTEM",
          "This CRD licence was converted to HDC licence on OPT_IN",
        ),
        tuple(
          hdcLicence.id,
          CRD_CREATED_WHEN_HDC_OPT_IN,
          "SYSTEM_USER",
          "SYSTEM",
          "SYSTEM",
          "This HDC licence was converted from CRD licence on OPT_IN",
        ),
      )
    assertThat(testRepository.findAllLicenceLinks())
      .hasSize(1)
      .extracting("fromLicenceId", "toLicenceId", "linkType")
      .containsExactly(tuple(crdLicence.id, hdcLicence.id, HDC_REPLACEMENT_FOR_OPTED_IN_LICENCE))
    verify(eventsPublisher, times(1)).publishDomainEvent(anyOrNull())
  }

  @SqlGroup(
    Sql("classpath:test_data/seed-completed-hdc-licence-1.sql"),
    Sql("classpath:test_data/seed-hdc-conversion-details.sql"),
  )
  @ParameterizedTest
  @CsvSource(
    "IN_PROGRESS, OPT_OUT",
    "SUBMITTED, OPT_OUT",
    "APPROVED, OPT_OUT",
    "IN_PROGRESS, REFUSE",
    "SUBMITTED, REFUSE",
    "APPROVED, REFUSE",
  )
  fun `An HDC conversion event converts a single eligible HDC licence to an in progress CRD licence`(
    status: LicenceStatus,
    eventType: HdcCvlEventType,
  ) {
    // Given
    testRepository.updateLicenceStatus(1L, status)
    val event = createHdcStatusChangedEvent()

    // When
    sendConversionEvent(event, eventType)

    // Then
    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)

    val hdcLicence = licences.single { it.kind == LicenceKind.HDC }
    assertThat(hdcLicence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

    val crdLicence =
      testRepository.findLicence(licences.single { it.kind == LicenceKind.CRD }.id) as CrdLicence

    assertCrdConversion(hdcLicence, crdLicence, eventType)

    assertThat(testRepository.findAllAuditEvents()).hasSize(4)
    assertThat(testRepository.findAllEventRepository())
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .containsExactly(
        tuple(
          1L,
          SUPERSEDED,
          "SYSTEM",
          "SYSTEM",
          "SYSTEM",
          "Licence automatically inactivated after HDC ${eventType.name} event for Person Three",
        ),
        tuple(
          1L,
          if (eventType == HdcCvlEventType.REFUSE) INACTIVE_WHEN_HDC_REFUSED else INACTIVE_WHEN_HDC_OPT_OUT,
          "SYSTEM_USER",
          "SYSTEM",
          "SYSTEM",
          "This HDC licence was converted to CRD licence on ${eventType.name}",
        ),
        tuple(
          2L,
          if (eventType == HdcCvlEventType.REFUSE) CRD_CREATED_WHEN_HDC_REFUSED else CRD_CREATED_WHEN_HDC_OPT_OUT,
          "SYSTEM_USER",
          "SYSTEM",
          "SYSTEM",
          "This CRD licence was converted from HDC licence on ${eventType.name}",
        ),
      )

    argumentCaptor<HMPPSDomainEvent>().apply {
      verify(eventsPublisher, times(1)).publishDomainEvent(capture())
    }
  }

  @SqlGroup(
    Sql("classpath:test_data/seed-hdc-approved-and-in-progress-version.sql"),
    Sql("classpath:test_data/seed-completed-hdc-licence-1.sql"),
    Sql("classpath:test_data/seed-hdc-conversion-details.sql"),
  )
  @ParameterizedTest
  @CsvSource(
    "IN_PROGRESS, OPT_OUT",
    "SUBMITTED, OPT_OUT",
    "IN_PROGRESS, REFUSE",
    "SUBMITTED, REFUSE",
  )
  fun `An HDC conversion event converts only the edit and inactivates the previously approved HDC licence`(
    editStatus: LicenceStatus,
    eventType: HdcCvlEventType,
  ) {
    // Given
    testRepository.updateLicenceStatus(2L, editStatus)
    val event = createHdcStatusChangedEvent()

    // When
    sendConversionEvent(event, eventType)

    // Then
    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(3)

    assertThat(licences[0].id).isEqualTo(1L)
    assertThat(licences[0].kind).isEqualTo(LicenceKind.HDC)
    assertThat(licences[0].statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licences[0].probationContact).isNotNull()

    assertThat(licences[1].id).isEqualTo(2L)
    assertThat(licences[1].kind).isEqualTo(LicenceKind.HDC)
    assertThat(licences[1].statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licences[1].probationContact).isNotNull()
    assertThat(licences[2].id).isEqualTo(3L)

    // converted hdc licence
    val crdLicence = testRepository.findLicence(3L) as CrdLicence
    assertCrdConversion(licences[1], crdLicence, eventType)

    val allEvents = testRepository.findAllEventRepository().sortedBy { it.licenceId }
    assertThat(allEvents)
      .extracting("licenceId", "eventType")
      .containsExactly(
        tuple(1L, SUPERSEDED),
        tuple(2L, SUPERSEDED),
        tuple(2L, if (eventType == HdcCvlEventType.REFUSE) INACTIVE_WHEN_HDC_REFUSED else INACTIVE_WHEN_HDC_OPT_OUT),
        tuple(3L, if (eventType == HdcCvlEventType.REFUSE) CRD_CREATED_WHEN_HDC_REFUSED else CRD_CREATED_WHEN_HDC_OPT_OUT),
      )

    val allAudits = testRepository.findAllAuditEvents().sortedBy { it.licenceId }
    assertThat(allAudits).hasSize(5)

    assertThat(allAudits[0].licenceId).isEqualTo(1L)
    assertThat(allAudits[0].summary).isEqualTo("Licence automatically inactivated after HDC ${eventType.name} event for Person Approved")
    assertThat(allAudits.filter { it.licenceId == 1L }).hasSize(1)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event with a single IN_PROGRESS licence leaves it at IN_PROGRESS`() {
    val event = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application postponed",
    )
    val eventJson = mapper.writeValueAsString(event)

    testRepository.updateLicenceStatus(1L, LicenceStatus.INACTIVE)
    testRepository.updateLicenceStatus(2L, LicenceStatus.IN_PROGRESS)

    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    assertThat(testRepository.findLicence(2L).statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(testRepository.findLicence(1L).statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(testRepository.findAllHdcPostponements())
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(tuple(2L, true))
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event with no edit underway and a submitted licence moves it to IN_PROGRESS`() {
    val event = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application postponed",
    )
    val eventJson = mapper.writeValueAsString(event)

    testRepository.updateLicenceStatus(1L, LicenceStatus.INACTIVE)

    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    val updatedSubmittedLicence = testRepository.findLicence(2L)
    assertThat(updatedSubmittedLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(updatedSubmittedLicence.approvedByUsername).isNull()
    assertThat(updatedSubmittedLicence.approvedByName).isNull()
    assertThat(updatedSubmittedLicence.approvedDate).isNull()
    assertThat(testRepository.findAllHdcPostponements())
      .extracting("licence.id", "isPostponed")
      .containsExactly(tuple(2L, true))

    assertThat(testRepository.findLicence(1L).statusCode).isEqualTo(LicenceStatus.INACTIVE)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event with no edit underway moves an approved licence to IN_PROGRESS`() {
    val event = createHdcStatusChangedEvent(
      licenceId = 1L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application postponed",
    )
    val eventJson = mapper.writeValueAsString(event)

    testRepository.updateLicenceStatus(2L, LicenceStatus.INACTIVE)

    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    val updatedApprovedLicence = testRepository.findLicence(1L)
    assertThat(updatedApprovedLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(updatedApprovedLicence.approvedByUsername).isNull()
    assertThat(updatedApprovedLicence.approvedByName).isNull()
    assertThat(updatedApprovedLicence.approvedDate).isNull()

    assertThat(testRepository.findLicence(2L).statusCode).isEqualTo(LicenceStatus.INACTIVE)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event with an approved edit underway inactivates the approved licence and moves the submitted edit to IN_PROGRESS`() {
    // Given
    val approvedLicence = testRepository.findLicence(1L)
    assertThat(approvedLicence.statusCode).isEqualTo(LicenceStatus.APPROVED)

    val event = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application postponed",
    )
    val eventJson = mapper.writeValueAsString(event)

    // When
    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    // Then
    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)

    val inactivatedApprovedLicence = testRepository.findLicence(1L)
    assertThat(inactivatedApprovedLicence.statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(inactivatedApprovedLicence.approvedByUsername).isEqualTo("approver.user")
    assertThat(inactivatedApprovedLicence.approvedByName).isEqualTo("Approved Approver")
    assertThat(inactivatedApprovedLicence.approvedDate).isNotNull

    val updatedSubmittedLicence = testRepository.findLicence(2L)
    assertThat(updatedSubmittedLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(updatedSubmittedLicence.kind).isEqualTo(LicenceKind.HDC)

    val auditEvents = testRepository.findAllAuditEvents()
    assertThat(auditEvents)
      .filteredOn { it.licenceId == 2L }
      .hasSize(2)
      .extracting("summary")
      .containsExactlyInAnyOrder(
        "HDC postponed - licence moved to IN_PROGRESS",
        "HDC postponement set for licence 2",
      )

    assertThat(auditEvents)
      .filteredOn { it.licenceId == 2L }
      .anySatisfy {
        assertThat(it.summary).isEqualTo("HDC postponed - licence moved to IN_PROGRESS")
        assertThat(it.detail).contains("SUBMITTED").contains("IN_PROGRESS")
      }

    assertThat(testRepository.findAllHdcPostponements())
      .hasSize(2)
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(
        tuple(1L, true),
        tuple(2L, true),
      )

    verify(eventsPublisher, times(1)).publishDomainEvent(anyOrNull())
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event with an approved and in progress edit inactivates the approved licence and keeps the in progress edit as IN_PROGRESS`() {
    val event = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application postponed",
    )
    val eventJson = mapper.writeValueAsString(event)

    testRepository.updateLicenceStatus(2L, LicenceStatus.IN_PROGRESS)

    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    val inactivatedApprovedLicence = testRepository.findLicence(1L)
    assertThat(inactivatedApprovedLicence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

    val inProgressEdit = testRepository.findLicence(2L)
    assertThat(inProgressEdit.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(testRepository.findAllHdcPostponements())
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(tuple(1L, true), tuple(2L, true))

    verify(eventsPublisher, times(1)).publishDomainEvent(anyOrNull())
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC resume event clears the postponement state for the current editable HDC licence`() {
    val postponeEvent = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application postponed",
    )
    val postponeEventJson = mapper.writeValueAsString(postponeEvent)

    val resumeEvent = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application resumed",
    )
    val resumeEventJson = mapper.writeValueAsString(resumeEvent)

    sendMessage(postponeEventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(postponeEventJson)
    verify(eventsPublisher, times(1)).publishDomainEvent(anyOrNull())

    assertThat(testRepository.findAllHdcPostponements())
      .hasSize(2)
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(
        tuple(1L, true),
        tuple(2L, true),
      )

    org.mockito.kotlin.clearInvocations(eventsPublisher)

    sendMessage(resumeEventJson, HdcCvlEventType.RESUME.toString())

    assertSqsProcessed(timesProcessed = 2)
    verify(hdcStatusChangedHandler).handleResume(resumeEventJson)

    assertThat(testRepository.findAllHdcPostponements())
      .hasSize(2)
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(tuple(1L, true), tuple(2L, false))

    assertThat(testRepository.findLicence(1L).statusCode).isEqualTo(LicenceStatus.INACTIVE)
    verifyNoInteractions(eventsPublisher)
  }

  private fun assertCrdConversion(hdcLicence: Licence, crdLicence: CrdLicence, eventType: HdcCvlEventType) {
    assertThat(crdLicence.kind).isEqualTo(LicenceKind.CRD)
    assertThat(crdLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(crdLicence.versionOfId).isEqualTo(hdcLicence.id)
    assertThat(crdLicence.licenceVersion).isEqualTo("1.0")
    assertThat(crdLicence.nomsId).isEqualTo(hdcLicence.nomsId)
    assertThat(crdLicence.bookingId).isEqualTo(hdcLicence.bookingId)
    assertThat(crdLicence.licenceStartDate).isEqualTo(hdcLicence.licenceStartDate)
    assertThat(crdLicence.probationContact).isNull()

    assertThat(crdLicence.additionalConditions).hasSize(1)
    assertThat(crdLicence.additionalConditions.single().expandedConditionText)
      .isEqualTo("Not to enter exclusion zone Town centre")
    assertThat(crdLicence.additionalConditions.single().additionalConditionData.single().dataValue)
      .isEqualTo("Town centre")
    assertThat(crdLicence.bespokeConditions).hasSize(1)
    assertThat(crdLicence.bespokeConditions.single().conditionText)
      .isEqualTo("Do not contact Person A")
    assertThat(crdLicence.standardConditions).hasSize(8)

    val events = testRepository.findAllAuditEvents()
    assertThat(events)
      .filteredOn { it.licenceId == crdLicence.id }
      .hasSize(2)
      .extracting("summary")
      .containsExactlyInAnyOrder(
        "Updated standard conditions to policy version ${crdLicence.version} for Person Three",
        "CRD licence converted from HDC on ${eventType.name}",
      )

    assertThat(events)
      .filteredOn { it.licenceId == hdcLicence.id }
      .hasSize(2)
      .extracting("summary")
      .containsExactlyInAnyOrder(
        "Licence automatically inactivated after HDC ${eventType.name} event for Person Three",
        "HDC licence converted to CRD licence on ${eventType.name}",
      )

    val conversionDetail =
      "Old ID ${hdcLicence.id}, new ID ${crdLicence.id} type ${crdLicence.typeCode} " +
        "status ${crdLicence.statusCode.name} version ${crdLicence.version}"

    assertThat(events)
      .filteredOn { it.licenceId == hdcLicence.id }
      .anySatisfy {
        assertThat(it.summary).isEqualTo("HDC licence converted to CRD licence on ${eventType.name}")
        assertThat(it.detail).isEqualTo(conversionDetail)
      }

    assertThat(events)
      .filteredOn { it.licenceId == crdLicence.id }
      .anySatisfy {
        assertThat(it.summary).isEqualTo("CRD licence converted from HDC on ${eventType.name}")
        assertThat(it.detail).isEqualTo(conversionDetail)
      }

    val links = testRepository.findAllLicenceLinks()
    val expectedLinkType = if (eventType == HdcCvlEventType.REFUSE) {
      CRD_REPLACEMENT_FOR_REFUSED_LICENCE
    } else {
      CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
    }
    assertThat(links)
      .hasSize(1)
      .extracting("fromLicenceId", "toLicenceId", "linkType")
      .containsExactly(tuple(hdcLicence.id, crdLicence.id, expectedLinkType))

    assertThat(links[0].dateCreated).isNotNull
  }

  private fun sendMessage(messageBody: String, eventType: String) {
    hdcCvlEventsSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(hdcCvlEventsQueueUrl)
        .messageBody(messageBody)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
          ),
        )
        .build(),
    )
  }

  private fun assertSqsProcessed(timesProcessed: Int = 1) {
    awaitAtMost30Secs untilAsserted {
      verify(hdcEventsListener, times(timesProcessed)).finishedEventProcessing(anyOrNull())
    }
    assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
  }

  private fun sendConversionEvent(event: HdcStatusChangedEvent, eventType: HdcCvlEventType): String {
    val eventJson = mapper.writeValueAsString(event)
    sendMessage(eventJson, eventType.toString())
    assertSqsProcessed()
    return eventJson
  }
  private fun createHdcStatusChangedEvent(
    licenceId: Long = 1L,
    bookingId: Long = 12347L,
    nomsNumber: String = "C1234CC",
    reason: String = "Offender opted out",
    triggeredBy: String = "test.user",
    occurredAt: LocalDateTime = LocalDateTime.now(),
  ) = HdcStatusChangedEvent(
    occurredAt = occurredAt,
    licenceId = licenceId,
    bookingId = bookingId,
    nomsNumber = nomsNumber,
    triggeredBy = triggeredBy,
    reason = reason,
  )
}
