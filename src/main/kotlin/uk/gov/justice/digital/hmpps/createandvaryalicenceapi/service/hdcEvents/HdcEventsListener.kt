package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.EventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Message

@ConditionalOnProperty(name = ["hdc.event.listener.disabled"], havingValue = "false", matchIfMissing = true)
@Service
class HdcEventsListener(
  private val hdcStatusChangedHandler: HdcStatusChangedHandler,
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log = LoggerFactory.getLogger(HdcEventsListener::class.java)
  }

  @SqsListener("hdccvleventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) {
    log.info("Raw HDC event message received: {}", rawMessage)
    try {
      val (message, messageId, messageAttributes) = mapper.readValue(rawMessage, Message::class.java)
      log.info("Successfully parsed message | messageId={} | eventType={}", messageId, messageAttributes.eventType.value)
      log.debug("Message body: {}", message)

      val eventType = try {
        HdcCvlEventType.valueOf(messageAttributes.eventType.value)
      } catch (e: IllegalArgumentException) {
        log.warn("Ignoring HDC event with unknown type {}", messageAttributes.eventType.value, e)
        return
      }

      log.info("Processing HDC event | eventType={}", eventType)
      when (eventType) {
        HdcCvlEventType.OPT_OUT -> hdcStatusChangedHandler.handleOptout(message)
        HdcCvlEventType.POSTPONE -> log.debug("POSTPONE event received but handler not yet implemented")
      }
      finishedEventProcessing(messageAttributes.eventType)
    } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
      log.error("Failed to parse HDC message - likely format mismatch. Raw message: {}", rawMessage, e)
      throw e
    }
  }

  fun finishedEventProcessing(eventType: EventType) {
    log.info("Processed HDC event: {}", eventType)
  }
}
