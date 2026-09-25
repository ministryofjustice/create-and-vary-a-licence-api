package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcMessage
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcMessageAttributes
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
    val event = HdcStatusChangedEvent(
      eventType = HdcCvlEventType.OPT_OUT,
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Offender opted out",
    )

    val eventJson = mapper.writeValueAsString(event)
    val wrappedMessage = HdcMessage(
      message = eventJson,
      messageAttributes = HdcMessageAttributes(eventType = HdcCvlEventType.OPT_OUT.toString()),
    )
    val messageBody = mapper.writeValueAsString(wrappedMessage)

    hdcCvlEventsSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(hdcCvlEventsQueueUrl)
        .messageBody(messageBody)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(HdcCvlEventType.OPT_OUT.toString()).build(),
          ),
        )
        .build(),
    )

    // Verify listener and handler are called, and queue is drained
    awaitAtMost30Secs untilAsserted {
      verify(hdcEventsListener, times(1)).finishedEventProcessing(HdcCvlEventType.OPT_OUT)
      verify(hdcStatusChangedHandler).handleOptout(eventJson)
    }
    assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
  }
}
