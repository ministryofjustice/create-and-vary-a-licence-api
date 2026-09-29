package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.Message
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.EventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Message
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.MessageAttributes

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
      val eventType = getEventType(messageAttributes)

      processedEventType = eventType
      log.info("Processing HDC event | eventType={}", eventType)

      when (eventType) {
        HdcCvlEventType.OPT_OUT -> hdcStatusChangedHandler.handleOptout(rawMessage)
        HdcCvlEventType.POSTPONE -> log.debug("POSTPONE event received but handler not yet implemented")
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      log.error("Failed to parse HDC message - likely format mismatch. Raw message: {}", rawMessage, e)
      throw e
    } finally {
      processedEventType?.let(::finishedEventProcessing)
    }
  }

  private fun getEventType(messageAttributes: MessageAttributes): HdcCvlEventType = HdcCvlEventType.valueOf(messageAttributes.eventType.value)

  fun finishedEventProcessing(eventType: EventType) {
    log.info("Processed HDC event: {}", eventType)
  }
}
