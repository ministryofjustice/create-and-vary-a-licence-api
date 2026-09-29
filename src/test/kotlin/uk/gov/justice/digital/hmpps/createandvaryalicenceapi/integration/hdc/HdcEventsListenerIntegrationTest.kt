package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.EventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Message
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.MessageAttributes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedHandler
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
    val (eventJson, messageBody) = createRequest()

    // When
    sendMessage(messageBody)

    // Then
    assertFinishedEventProcessing()

    verify(hdcStatusChangedHandler).handleOptout(eventJson)
    assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
  }

  @SqlGroup(
    Sql("classpath:test_data/seed-completed-hdc-licence-1.sql"),
    Sql("classpath:test_data/seed-hdc-conversion-details.sql"),
  )
  @Test
  fun `An HDC opt out event is processed with a existing HDC licence`() {
    // Given
    val (eventJson, messageBody) = createRequest(licenceId = 1L, bookingId = 12347L, nomsNumber = "C1234CC")

    // When
    sendMessage(messageBody)

    // Then
    assertFinishedEventProcessing()
    verify(hdcStatusChangedHandler).handleOptout(eventJson)
    assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
  }

  private fun assertFinishedEventProcessing() {
    awaitAtMost30Secs untilAsserted {
      verify(hdcEventsListener, times(1)).finishedEventProcessing(any())
    }
  }

  private fun sendMessage(messageBody: String) {
    hdcCvlEventsSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(hdcCvlEventsQueueUrl)
        .messageBody(messageBody)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(HdcCvlEventType.OPT_OUT.toString()).build(),
          ),
        )
        .build(),
    )
  }

  private fun createRequest(
    occurredAt: LocalDateTime = LocalDateTime.now(),
    licenceId: Long = 123L,
    bookingId: Long = 456L,
    nomsNumber: String = "A1234BC",
    triggeredBy: String = "test.user",
    reason: String = "Offender opted out",
    eventType: HdcCvlEventType = HdcCvlEventType.OPT_OUT,
  ): Pair<String, String> {
    val event = HdcStatusChangedEvent(
      occurredAt = occurredAt,
      licenceId = licenceId,
      bookingId = bookingId,
      nomsNumber = nomsNumber,
      triggeredBy = triggeredBy,
      reason = reason,
      eventType = eventType,
    )

    val eventJson = mapper.writeValueAsString(event)

    val wrappedMessage = Message(
      message = eventJson,
      messageId = "test-message-id",
      messageAttributes = MessageAttributes(
        eventType = EventType(
          value = eventType.toString(),
          type = "String",
        ),
      ),
    )

    val messageBody = mapper.writeValueAsString(wrappedMessage)

    return Pair(eventJson, messageBody)
  }
}
