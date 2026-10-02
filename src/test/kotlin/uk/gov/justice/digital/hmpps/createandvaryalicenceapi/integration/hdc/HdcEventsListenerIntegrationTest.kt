package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Duration
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["hdc.event.listener.disabled=false"])
class HdcEventsListenerIntegrationTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var hdcEventsListener: HdcEventsListener

  @MockitoSpyBean
  lateinit var hdcStatusChangedHandler: HdcStatusChangedHandler

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
  }

  @SqlGroup(
    Sql("classpath:test_data/seed-completed-hdc-licence-1.sql"),
    Sql("classpath:test_data/seed-hdc-conversion-details.sql"),
  )
  @Test
  fun `An HDC opt out event is processed with a existing HDC licence`() {
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
    val crdLicence = testRepository.findLicence(licences.single { it.kind == LicenceKind.CRD }.id) as CrdLicence
    assertThat(crdLicence.statusCode).isEqualTo(LicenceStatus.IN_PROGRESS)
    assertThat(crdLicence.versionOfId).isEqualTo(hdcLicence.id)
    assertThat(crdLicence.licenceVersion).isEqualTo("1.1")
    assertThat(crdLicence.nomsId).isEqualTo(hdcLicence.nomsId)
    assertThat(crdLicence.bookingId).isEqualTo(hdcLicence.bookingId)
    assertThat(crdLicence.licenceStartDate).isEqualTo(hdcLicence.licenceStartDate)
    assertThat(crdLicence.probationContact).isNull()

    assertThat(crdLicence.additionalConditions).hasSize(1)
    assertThat(crdLicence.additionalConditions.single().expandedConditionText)
      .isEqualTo("Not to enter exclusion zone Town centre")
    assertThat(crdLicence.additionalConditions.single().additionalConditionData.single().dataValue)
      .isEqualTo("Town centre")
    assertThat(crdLicence.bespokeConditions.single().conditionText)
      .isEqualTo("Do not contact Person A")

    val auditEvent = testRepository.findFirstAuditEvent(hdcLicence.id)
    assertThat(auditEvent.summary)
      .isEqualTo("Hdc licence converted to CRD licence for ${crdLicence.forename} ${crdLicence.surname}")
    assertThat(auditEvent.detail)
      .isEqualTo(
        "Old ID ${hdcLicence.id}, new ID ${crdLicence.id} type ${crdLicence.typeCode} " +
          "status ${crdLicence.statusCode.name} version ${crdLicence.version}",
      )
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
}
