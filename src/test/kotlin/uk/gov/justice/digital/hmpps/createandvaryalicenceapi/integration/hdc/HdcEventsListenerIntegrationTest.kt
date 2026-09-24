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
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.EventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HDC_OPT_OUT_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HdcStatusChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HdcStatusChangedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Message
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.MessageAttributes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcEventsListener
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
    val event = HdcStatusChangedEvent(
      eventType = HDC_OPT_OUT_EVENT_TYPE,
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Offender opted out",
    )

    val eventJson = mapper.writeValueAsString(event)
    val wrappedMessage = Message(
      message = eventJson,
      messageId = "test-message-id",
      messageAttributes = MessageAttributes(eventType = EventType(value = HDC_OPT_OUT_EVENT_TYPE, type = "String")),
    )
    val messageBody = mapper.writeValueAsString(wrappedMessage)

    hdcCvlEventsSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(hdcCvlEventsQueueUrl)
        .messageBody(messageBody)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(HDC_OPT_OUT_EVENT_TYPE).build(),
          ),
        )
        .build(),
    )

    // Verify listener and handler are called, and queue is drained
    awaitAtMost30Secs untilAsserted {
      verify(hdcEventsListener, times(1)).finishedEventProcessing(any())
      verify(hdcStatusChangedHandler).handleOptout(eventJson)
      assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
    }
  }
}
