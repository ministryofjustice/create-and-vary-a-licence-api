package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.springframework.messaging.support.GenericMessage
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

class HdcEventsListenerTest {
  private val mapper = spy(ObjectMapper())
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

  @Test
  fun `onMessage processes REFUSED event successfully`() {
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "HDC application refused",
    )
    val eventJson = mapper.writeValueAsString(event)
    val message = GenericMessage(eventJson, mapOf("eventType" to "REFUSE"))

    listener.onMessage(message)

    verify(handler).handleRefuse(eventJson)
  }

  @Test
  fun `onMessage processes POSTPONE event successfully`() {
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Test",
    )
    val eventJson = mapper.writeValueAsString(event)

    val headers = mapOf<String, Any>("eventType" to "POSTPONE").toMutableMap()
    val message = GenericMessage(eventJson, headers)

    listener.onMessage(message)

    val captor = argumentCaptor<String>()
    verify(handler).handlePostpone(captor.capture())
    assertThat(captor.firstValue).isEqualTo(eventJson)
  }

  @Test
  fun `onMessage processes RESUME event successfully`() {
    val event = HdcStatusChangedEvent(
      occurredAt = LocalDateTime.now(),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Test",
    )
    val eventJson = mapper.writeValueAsString(event)

    val headers = mapOf<String, Any>("eventType" to "RESUME").toMutableMap()
    val message = GenericMessage(eventJson, headers)

    listener.onMessage(message)

    val captor = argumentCaptor<String>()
    verify(handler).handleResume(captor.capture())
    assertThat(captor.firstValue).isEqualTo(eventJson)
  }
}
