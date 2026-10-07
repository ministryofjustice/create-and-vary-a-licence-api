package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

const val COM_ALLOCATED_EVENT_TYPE = "person.community.manager.allocated"
const val PRISONER_UPDATED_EVENT_TYPE = "prisoner-offender-search.prisoner.updated"
const val RECALL_INSERTED_EVENT_TYPE = "recall.inserted"
const val RECALL_UPDATED_EVENT_TYPE = "recall.updated"
const val PRISON_OFFENDER_MERGED_EVENT_TYPE = "prison-offender-events.prisoner.merged"
const val PRISON_OFFENDER_RECEIVED_EVENT_TYPE = "prison-offender-events.prisoner.received"
const val PRISON_OFFENDER_RELEASED_EVENT_TYPE = "prisoner-offender-search.prisoner.released"

@ConditionalOnProperty(name = ["domain.event.listener.enabled"], havingValue = "true", matchIfMissing = true)
@Service
class DomainEventListener(
  comAllocatedHandler: ComAllocatedHandler,
  prisonerUpdatedHandler: PrisonerUpdatedHandler,
  recallInsertedHandler: RecallInsertedHandler,
  recallUpdatedHandler: RecallUpdatedHandler,
  prisonerMergedHandler: PrisonerMergedHandler,
  prisonerReceivedHandler: PrisonerReceivedHandler,
  prisonerReleasedHandler: PrisonerReleasedHandler,

  private val mapper: ObjectMapper,
) {
  private val eventTypeToHandler = mapOf(
    COM_ALLOCATED_EVENT_TYPE to comAllocatedHandler,
    PRISONER_UPDATED_EVENT_TYPE to prisonerUpdatedHandler,
    RECALL_INSERTED_EVENT_TYPE to recallInsertedHandler,
    RECALL_UPDATED_EVENT_TYPE to recallUpdatedHandler,
    PRISON_OFFENDER_MERGED_EVENT_TYPE to prisonerMergedHandler,
    PRISON_OFFENDER_RECEIVED_EVENT_TYPE to prisonerReceivedHandler,
    PRISON_OFFENDER_RELEASED_EVENT_TYPE to prisonerReleasedHandler,
  )

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    val (message, _, messageAttributes) = mapper.readValue(rawMessage, Message::class.java)

    try {
      val eventType = messageAttributes.eventType.value
      val handler = eventTypeToHandler[eventType]

      if (handler != null) {
        handler.handleEvent(message)
      } else {
        log.warn("Ignoring message with type $eventType as no handler was found")
      }
    } finally {
      finishedEventProcessing(messageAttributes.eventType)
    }
  }

  fun finishedEventProcessing(eventType: EventType) {
    log.info("Processed event: $eventType")
  }
}
