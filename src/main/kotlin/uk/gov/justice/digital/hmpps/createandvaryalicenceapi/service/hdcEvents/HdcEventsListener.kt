package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.Message
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

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
  fun onMessage(message: Message<String>) {
    val rawMessage = message.payload
    log.info("Raw HDC event message received: {}", rawMessage)

    var processedEventType: HdcCvlEventType? = null
    try {
      val event = mapper.readValue(rawMessage, HdcStatusChangedEvent::class.java)
      val eventTypeValue = requireNotNull(message.headers["eventType"] as? String) {
        "Missing eventType in message attributes"
      }

      log.info("Successfully parsed HDC event | eventType={} | licenceId={}", eventTypeValue, event.licenceId)

      val eventType = runCatching {
        HdcCvlEventType.valueOf(eventTypeValue)
      }.getOrElse { e ->
        log.warn("Ignoring HDC event with unknown type {}", eventTypeValue, e)
        return
      }

      processedEventType = eventType
      log.info("Processing HDC event | eventType={}", eventType)

      when (eventType) {
        HdcCvlEventType.OPT_OUT -> hdcStatusChangedHandler.handleOptout(rawMessage, eventType)
        HdcCvlEventType.POSTPONE -> log.debug("POSTPONE event received but handler not yet implemented")
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      log.error("Failed to parse HDC message - likely format mismatch. Raw message: {}", rawMessage, e)
      throw e
    } finally {
      processedEventType?.let(::finishedEventProcessing)
    }
  }

  fun finishedEventProcessing(eventType: HdcCvlEventType) {
    log.info("Processed HDC event: {}", eventType)
  }
}
