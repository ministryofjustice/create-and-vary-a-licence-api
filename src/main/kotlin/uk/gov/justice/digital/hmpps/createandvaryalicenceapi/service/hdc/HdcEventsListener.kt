package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.EventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HDC_OPT_OUT_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HdcStatusChangedHandler
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
    val (message, _, messageAttributes) = mapper.readValue(rawMessage, Message::class.java)

    try {
      when (val eventType = messageAttributes.eventType.value) {
        HDC_OPT_OUT_EVENT_TYPE -> hdcStatusChangedHandler.handleEvent(message)
        else -> log.warn("Ignoring HDC event with type {}", eventType)
      }
    } finally {
      finishedEventProcessing(messageAttributes.eventType)
    }
  }

  fun finishedEventProcessing(eventType: EventType) {
    log.info("Processed HDC event: {}", eventType)
  }
}
