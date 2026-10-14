package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceLinkType.CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.SUPERSEDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
    verify(hdcStatusChangedHandler).handleOptout(eventJson)
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
    verify(hdcStatusChangedHandler, times(0)).handleOptout(eventJson)
    assertThat(testRepository.findAllAuditEvents()).isEmpty()
    verifyNoInteractions(eventsPublisher)
  }

  @SqlGroup(
    Sql("classpath:test_data/seed-completed-hdc-licence-1.sql"),
    Sql("classpath:test_data/seed-hdc-conversion-details.sql"),
  )
  @ParameterizedTest
  @EnumSource(LicenceStatus::class, names = ["IN_PROGRESS", "SUBMITTED", "APPROVED"])
  fun `An HDC opt out event converts a single eligible HDC licence to an in progress CRD licence`(status: LicenceStatus) {
    // Given
    testRepository.updateLicenceStatus(1L, status)
    val event = createHdcStatusChangedEvent()

    // When
    sendOptOutEvent(event)

    // Then
    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)

    val hdcLicence = licences.single { it.kind == LicenceKind.HDC }
    assertThat(hdcLicence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

    val crdLicence =
      testRepository.findLicence(licences.single { it.kind == LicenceKind.CRD }.id) as CrdLicence

    assertCrdConversion(hdcLicence, crdLicence)

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
          "Licence automatically inactivated after HDC opt out event for Person Three",
        ),
        tuple(
          1L,
          INACTIVE_WHEN_HDC_OPT_OUT,
          "SYSTEM_USER",
          "SYSTEM",
          "SYSTEM",
          "This HDC licence was converted to CRD licence on Opt Out",
        ),
        tuple(
          2L,
          CRD_CREATED_WHEN_HDC_OPT_OUT,
          "SYSTEM_USER",
          "SYSTEM",
          "SYSTEM",
          "This CRD Licence was converted from Hdc a licence on Opt Out",
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
  @EnumSource(LicenceStatus::class, names = ["IN_PROGRESS", "SUBMITTED"])
  fun `An HDC opt out event converts only the edit and inactivates the previously approved HDC licence`(editStatus: LicenceStatus) {
    // Given
    testRepository.updateLicenceStatus(2L, editStatus)
    val event = createHdcStatusChangedEvent()

    // When
    sendOptOutEvent(event)

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
    assertCrdConversion(licences[1], crdLicence)

    val allEvents = testRepository.findAllEventRepository().sortedBy { it.licenceId }
    assertThat(allEvents)
      .extracting("licenceId", "eventType")
      .containsExactly(
        tuple(1L, SUPERSEDED),
        tuple(2L, SUPERSEDED),
        tuple(2L, INACTIVE_WHEN_HDC_OPT_OUT),
        tuple(3L, CRD_CREATED_WHEN_HDC_OPT_OUT),
      )

    val allAudits = testRepository.findAllAuditEvents().sortedBy { it.licenceId }
    assertThat(allAudits).hasSize(5)

    assertThat(allAudits[0].licenceId).isEqualTo(1L)
    assertThat(allAudits[0].summary).isEqualTo("Licence automatically inactivated after HDC opt out event for Person Approved")
    assertThat(allAudits.filter { it.licenceId == 1L }).hasSize(1)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event on a submitted in-flight licence moves it to IN_PROGRESS and leaves the approved licence untouched`() {
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

    val unchangedApprovedLicence = testRepository.findLicence(1L)
    assertThat(unchangedApprovedLicence.statusCode).isEqualTo(LicenceStatus.APPROVED)
    assertThat(unchangedApprovedLicence.approvedByUsername).isEqualTo("approver.user")
    assertThat(unchangedApprovedLicence.approvedByName).isEqualTo("Approved Approver")
    assertThat(unchangedApprovedLicence.approvedDate).isNotNull

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

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC resume event clears the current postponement state for the submitted edit without changing the approved licence`() {
    val event = createHdcStatusChangedEvent(
      licenceId = 2L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      reason = "HDC application resumed",
    )
    val eventJson = mapper.writeValueAsString(event)

    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    assertThat(testRepository.findAllHdcPostponements())
      .hasSize(2)
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(
        tuple(1L, true),
        tuple(2L, true),
      )

    sendMessage(eventJson, HdcCvlEventType.RESUME.toString())

    assertSqsProcessed(timesProcessed = 2)
    verify(hdcStatusChangedHandler).handleResume(eventJson)

    assertThat(testRepository.findAllHdcPostponements())
      .hasSize(2)
      .extracting("licence.id", "isPostponed")
      .containsExactlyInAnyOrder(
        tuple(1L, false),
        tuple(2L, false),
      )

    assertThat(testRepository.findLicence(1L).statusCode).isEqualTo(LicenceStatus.APPROVED)
    verifyNoInteractions(eventsPublisher)
  }

  private fun assertCrdConversion(hdcLicence: Licence, crdLicence: CrdLicence) {
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
        "CRD licence converted from HDC on Opt Out",
      )

    assertThat(events)
      .filteredOn { it.licenceId == hdcLicence.id }
      .hasSize(2)
      .extracting("summary")
      .containsExactlyInAnyOrder(
        "Licence automatically inactivated after HDC opt out event for Person Three",
        "Hdc licence converted to CRD licence on Opt Out",
      )

    val conversionDetail =
      "Old ID ${hdcLicence.id}, new ID ${crdLicence.id} type ${crdLicence.typeCode} " +
        "status ${crdLicence.statusCode.name} version ${crdLicence.version}"

    assertThat(events)
      .filteredOn { it.licenceId == hdcLicence.id }
      .anySatisfy {
        assertThat(it.summary).isEqualTo("Hdc licence converted to CRD licence on Opt Out")
        assertThat(it.detail).isEqualTo(conversionDetail)
      }

    assertThat(events)
      .filteredOn { it.licenceId == crdLicence.id }
      .anySatisfy {
        assertThat(it.summary).isEqualTo("CRD licence converted from HDC on Opt Out")
        assertThat(it.detail).isEqualTo(conversionDetail)
      }

    val links = testRepository.findAllLicenceLinks()
    assertThat(links)
      .filteredOn { it.linkType == CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE }
      .hasSize(1)
      .extracting("fromLicenceId", "toLicenceId")
      .containsExactly(tuple(hdcLicence.id, crdLicence.id))
    assertThat(links.single { it.linkType == CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE }.dateCreated).isNotNull
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

  private fun sendOptOutEvent(event: HdcStatusChangedEvent): String? {
    val eventJson = mapper.writeValueAsString(event)
    sendMessage(eventJson, HdcCvlEventType.OPT_OUT.toString())
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
