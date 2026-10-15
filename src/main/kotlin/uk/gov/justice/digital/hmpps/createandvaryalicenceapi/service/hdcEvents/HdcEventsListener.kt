package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.Message
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.POSTPONE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.REFUSE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.RESUME

@ConditionalOnProperty(name = ["hdc.event.listener.enabled"], havingValue = "true", matchIfMissing = true)
@Service
class HdcEventsListener(
  private val hdcStatusChangedHandler: HdcStatusChangedHandler,
  private val mapper: ObjectMapper,
) {
  private val log = LoggerFactory.getLogger(HdcEventsListener::class.java)

  @SqsListener("hdccvleventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(message: Message<String>) {
    val rawMessage = message.payload
    log.debug("Raw HDC event message received: {}", rawMessage)

    var eventType: HdcCvlEventType? = null
    try {
      val event = mapper.readValue(rawMessage, HdcStatusChangedEvent::class.java)
      eventType = getHdcEventType(message)
      log.debug("Successfully parsed HDC event | eventType={} | licenceId={}", eventType, event.licenceId)
      when (eventType) {
        OPT_OUT -> hdcStatusChangedHandler.handleOptout(rawMessage)
        REFUSE -> hdcStatusChangedHandler.handleRefused(rawMessage)
        POSTPONE -> hdcStatusChangedHandler.handlePostpone(rawMessage)
        RESUME -> hdcStatusChangedHandler.handleResume(rawMessage)
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      log.error("Failed to parse HDC message - likely format mismatch. Raw message: {}", rawMessage, e)
      throw e
    } finally {
      finishedEventProcessing(eventType)
    }
  }

  private fun getHdcEventType(
    message: Message<String>,
  ): HdcCvlEventType {
    val eventTypeValue = requireNotNull(message.headers["eventType"] as? String) {
      "Missing eventType in message attributes"
    }
    return HdcCvlEventType.valueOf(eventTypeValue)
  }

  fun finishedEventProcessing(eventType: HdcCvlEventType?) {
    log.info("Processed HDC event: {}", eventType)
  }
}
