package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
  fun onMessage(rawMessage: String) {
    val (message, _, messageAttributes) = mapper.readValue(rawMessage, Message::class.java)

    try {
      val eventType = getEventType(messageAttributes)

      when (eventType) {
        HdcCvlEventType.OPT_OUT -> hdcStatusChangedHandler.handleOptout(message)
        HdcCvlEventType.POSTPONE -> log.debug("POSTPONE event received but handler not yet implemented")
      }
    } finally {
      finishedEventProcessing(messageAttributes.eventType)
    }
  }

  private fun getEventType(messageAttributes: MessageAttributes): HdcCvlEventType = HdcCvlEventType.valueOf(messageAttributes.eventType.value)

  fun finishedEventProcessing(eventType: EventType) {
    log.info("Processed HDC event: {}", eventType)
  }
}
