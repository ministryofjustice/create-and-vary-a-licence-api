package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
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
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Offender opted out",
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
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
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
  @Test
  fun `An HDC opt out event is processed with an existing HDC licence`() {
    // Given
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 1L,
      bookingId = 12347L,
      nomsNumber = "C1234CC",
      triggeredBy = "test.user",
      reason = "Offender opted out",
    )
    val eventJson = mapper.writeValueAsString(event)

    // When
    sendMessage(eventJson, HdcCvlEventType.OPT_OUT.toString())

    // Then
    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handleOptout(eventJson)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)

    val hdcLicence = licences.single { it.kind == LicenceKind.HDC }
    assertThat(hdcLicence.statusCode).isEqualTo(LicenceStatus.INACTIVE)

    val crdLicence =
      testRepository.findLicence(licences.single { it.kind == LicenceKind.CRD }.id) as CrdLicence

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
  @Test
  fun `An HDC opt out event is processed with an approved HDC licence and an in progress version`() {
    // Given
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 1L,
      bookingId = 12347L,
      nomsNumber = "C1234CC",
      triggeredBy = "test.user",
      reason = "Offender opted out",
    )
    val eventJson = mapper.writeValueAsString(event)

    // When
    sendMessage(eventJson, HdcCvlEventType.OPT_OUT.toString())

    // Then
    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handleOptout(eventJson)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(4)

    assertThat(licences[0].id).isEqualTo(1L)
    assertThat(licences[0].statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licences[0].probationContact).isNotNull()

    assertThat(licences[1].id).isEqualTo(2L)
    assertThat(licences[1].statusCode).isEqualTo(LicenceStatus.INACTIVE)
    assertThat(licences[1].probationContact).isNotNull()

    assertThat(licences[2].id).isEqualTo(3L)
    assertThat(licences[2].statusCode).isEqualTo(LicenceStatus.APPROVED)
    assertThat(licences[2].probationContact).isNull()

    assertThat(licences[3].id).isEqualTo(4L)
    assertThat(licences[3].statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licences[3].probationContact).isNull()

    val allEvents = testRepository.findAllEventRepository().sortedBy { it.licenceId }
    assertThat(allEvents).hasSize(6)
    assertThat(allEvents[0].eventType).isEqualTo(SUPERSEDED)
    assertThat(allEvents[1].eventType).isEqualTo(INACTIVE_WHEN_HDC_OPT_OUT)
    assertThat(allEvents[2].eventType).isEqualTo(SUPERSEDED)
    assertThat(allEvents[3].eventType).isEqualTo(INACTIVE_WHEN_HDC_OPT_OUT)
    assertThat(allEvents[4].eventType).isEqualTo(CRD_CREATED_WHEN_HDC_OPT_OUT)
    assertThat(allEvents[5].eventType).isEqualTo(CRD_CREATED_WHEN_HDC_OPT_OUT)

    val allAudits = testRepository.findAllAuditEvents().sortedBy { it.licenceId }
    assertThat(allAudits).hasSize(8)

    assertThat(allAudits[0].licenceId).isEqualTo(1L)
    assertThat(allAudits[0].summary).isEqualTo("Licence automatically inactivated after HDC opt out event for Person Approved")
    assertThat(allAudits[1].licenceId).isEqualTo(1L)
    assertThat(allAudits[1].summary).isEqualTo("Hdc licence converted to CRD licence on Opt Out")

    assertThat(allAudits[2].licenceId).isEqualTo(2L)
    assertThat(allAudits[2].summary).isEqualTo("Licence automatically inactivated after HDC opt out event for Person Three")
    assertThat(allAudits[3].licenceId).isEqualTo(2L)
    assertThat(allAudits[3].summary).isEqualTo("Hdc licence converted to CRD licence on Opt Out")

    assertThat(allAudits[4].licenceId).isEqualTo(3L)
    assertThat(allAudits[4].summary).isEqualTo("Updated standard conditions to policy version 4.1 for Person Approved")
    assertThat(allAudits[5].licenceId).isEqualTo(3L)
    assertThat(allAudits[5].summary).isEqualTo("CRD licence converted from HDC on Opt Out")

    assertThat(allAudits[6].licenceId).isEqualTo(4L)
    assertThat(allAudits[6].summary).isEqualTo("Updated standard conditions to policy version 4.1 for Person Three")
    assertThat(allAudits[7].licenceId).isEqualTo(4L)
    assertThat(allAudits[7].summary).isEqualTo("CRD licence converted from HDC on Opt Out")
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

  private fun assertSqsProcessed() {
    awaitAtMost30Secs untilAsserted {
      verify(hdcEventsListener, times(1)).finishedEventProcessing(anyOrNull())
    }
    assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event transitions licence from APPROVED to IN_PROGRESS`() {
    // Given
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 1L,
      bookingId = 12345L,
      nomsNumber = "A1234AA",
      triggeredBy = "test.user",
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

    val licence = testRepository.findLicence(1L)
    assertThat(licence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licence.kind).isEqualTo(LicenceKind.HDC)
    assertThat(licence.approvedByUsername).isNull()
    assertThat(licence.approvedByName).isNull()
    assertThat(licence.approvedDate).isNull()

    val auditEvents = testRepository.findAllAuditEvents()
    assertThat(auditEvents)
      .filteredOn { it.licenceId == 1L }
      .hasSize(1)
      .anySatisfy {
        assertThat(it.summary).isEqualTo("HDC postponed - licence moved to IN_PROGRESS")
        assertThat(it.detail).contains("APPROVED").contains("IN_PROGRESS")
      }

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `An HDC postpone event on a submitted in-flight licence updates it to IN_PROGRESS without creating a second licence`() {
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 2L,
      bookingId = 12346L,
      nomsNumber = "A1234AB",
      triggeredBy = "test.user",
      reason = "HDC application postponed",
    )
    val eventJson = mapper.writeValueAsString(event)

    sendMessage(eventJson, HdcCvlEventType.POSTPONE.toString())

    assertSqsProcessed()
    verify(hdcStatusChangedHandler).handlePostpone(eventJson)

    val licences = testRepository.findAllLicence()
    assertThat(licences).hasSize(2)
    assertThat(licences.single { it.id == 2L }.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(licences.single { it.id == 2L }.kind).isEqualTo(LicenceKind.HDC)
    val auditEvents = testRepository.findAllAuditEvents()
    assertThat(auditEvents)
      .filteredOn { it.licenceId == 2L }
      .hasSize(1)
      .anySatisfy {
        assertThat(it.summary).isEqualTo("HDC postponed - licence moved to IN_PROGRESS")
        assertThat(it.detail).contains("SUBMITTED").contains("IN_PROGRESS")
      }
    verifyNoInteractions(eventsPublisher)
  }
}
