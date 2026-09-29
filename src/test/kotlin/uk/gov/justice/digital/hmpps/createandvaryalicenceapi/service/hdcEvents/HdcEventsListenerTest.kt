package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.messaging.support.GenericMessage
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

class HdcEventsListenerTest {
  private val mapper = ObjectMapper()
  private val handler = mock<HdcStatusChangedHandler>()
  private val listener = HdcEventsListener(handler, mapper)

  @Test
  fun `onMessage throws exception for unknown event type`() {
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Test",
    )
    val eventJson = mapper.writeValueAsString(event)

    val headers = mapOf<String, Any>("eventType" to "UNKNOWN_TYPE").toMutableMap()
    val message = GenericMessage(eventJson, headers)

    val exception = assertThrows<IllegalArgumentException> {
      listener.onMessage(message)
    }

    assertThat(exception.message).contains("No enum constant")
  }

  @Test
  fun `onMessage processes OPT_OUT event successfully`() {
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Test",
    )
    val eventJson = mapper.writeValueAsString(event)

    val headers = mapOf<String, Any>("eventType" to "OPT_OUT").toMutableMap()
    val message = GenericMessage(eventJson, headers)

    listener.onMessage(message)

    val captor = argumentCaptor<String>()
    verify(handler).handleOptout(captor.capture())
    assertThat(captor.firstValue).isEqualTo(eventJson)
  }
}
