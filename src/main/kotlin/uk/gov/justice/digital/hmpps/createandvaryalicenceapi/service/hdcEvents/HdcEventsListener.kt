package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
  fun onMessage(rawMessage: String) {
    var eventType: HdcCvlEventType? = null
    try {
      log.info("Raw HDC event message received: {}", rawMessage)
      val event = mapper.readValue(rawMessage, HdcStatusChangedEvent::class.java)
      log.info("Successfully parsed HDC event | eventType={} | licenceId={}", event.eventType, event.licenceId)

      eventType = event.eventType

      log.info("Processing HDC event | eventType={}", eventType)
      when (eventType) {
        HdcCvlEventType.OPT_OUT -> hdcStatusChangedHandler.handleOptout(rawMessage)
        HdcCvlEventType.POSTPONE -> log.debug("POSTPONE event received but handler not yet implemented")
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      log.error("Failed to parse HDC message - likely format mismatch. Raw message: {}", rawMessage, e)
      throw e
    } finally {
      finishedEventProcessing(eventType)
    }
  }

  fun finishedEventProcessing(eventType: HdcCvlEventType?) {
    log.info("Processed HDC event: {}", eventType)
  }
}
