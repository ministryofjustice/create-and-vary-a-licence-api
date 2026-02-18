package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.EventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Message

const val SENTENCE_DATES_CHANGED_EVENT_TYPE = "SENTENCE_DATES-CHANGED"
const val CONFIRMED_RELEASE_DATE_CHANGED_EVENT_TYPE = "CONFIRMED_RELEASE_DATE-CHANGED"

@ConditionalOnProperty(name = ["prison.event.listener.enabled"], havingValue = "true", matchIfMissing = false)
@Service
class PrisonEventsListener(
  private val sentenceDatesChangedHandler: SentenceDatesChangedHandler,
  private val objectMapper: ObjectMapper,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("prisoneventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    val (message, _, messageAttributes) = objectMapper.readValue<Message>(rawMessage)

    when (val eventType = messageAttributes.eventType.value) {
      SENTENCE_DATES_CHANGED_EVENT_TYPE, CONFIRMED_RELEASE_DATE_CHANGED_EVENT_TYPE -> {
        sentenceDatesChangedHandler.handleEvent(message)
      }

      else -> {
        log.warn("Ignoring message with type $eventType")
      }
    }
    finishedEventProcessing(messageAttributes.eventType)
  }

  fun finishedEventProcessing(eventType: EventType) {
    log.info("Processed event: $eventType")
  }
}
