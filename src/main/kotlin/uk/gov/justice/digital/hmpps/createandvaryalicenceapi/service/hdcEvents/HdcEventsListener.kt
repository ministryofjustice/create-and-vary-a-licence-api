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
    log.debug("HDC event message received: {}", rawMessage)

    var processedEventType: HdcCvlEventType? = null
    try {
      val event = mapper.readValue(rawMessage, HdcStatusChangedEvent::class.java)
      val eventType = getHdcEventType(message)

      log.debug("Successfully parsed HDC event | eventType={} | licenceId={}", eventType, event.licenceId)

      processedEventType = eventType

      when (eventType) {
        HdcCvlEventType.OPT_OUT -> hdcStatusChangedHandler.handleOptout(rawMessage)
        HdcCvlEventType.POSTPONE -> log.warn("POSTPONE event received but handler not yet implemented - licenceId={}", event.licenceId)
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      log.error("Failed to process HDC message", e)
      throw e
    } finally {
      processedEventType?.let(::finishedEventProcessing)
    }
  }

  private fun getHdcEventType(message: Message<String>): HdcCvlEventType {
    val eventTypeValue = requireNotNull(message.headers["eventType"] as? String) {
      "Missing eventType in message attributes"
    }
    return runCatching {
      HdcCvlEventType.valueOf(eventTypeValue)
    }.getOrElse { e ->
      log.error("Unknown HDC event type: {}", eventTypeValue, e)
      throw e
    }
  }

  fun finishedEventProcessing(eventType: HdcCvlEventType) {
    log.info("Processed HDC event: {}", eventType)
  }
}
