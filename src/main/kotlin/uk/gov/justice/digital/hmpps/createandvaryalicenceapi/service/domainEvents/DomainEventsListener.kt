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

@ConditionalOnProperty(name = ["domain.event.listener.disabled"], havingValue = "false", matchIfMissing = true)
@Service
class DomainEventListener(
  private val comAllocatedHandler: ComAllocatedHandler,
  private val prisonerUpdatedHandler: PrisonerUpdatedHandler,
  private val recallInsertedHandler: RecallInsertedHandler,
  private val recallUpdatedHandler: RecallUpdatedHandler,
  private val mapper: ObjectMapper,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    val (message, _, messageAttributes) = mapper.readValue(rawMessage, Message::class.java)

    when (val eventType = messageAttributes.eventType.value) {
      COM_ALLOCATED_EVENT_TYPE -> {
        comAllocatedHandler.handleEvent(message)
      }

      PRISONER_UPDATED_EVENT_TYPE -> {
        prisonerUpdatedHandler.handleEvent(message)
      }

      RECALL_INSERTED_EVENT_TYPE -> {
        recallInsertedHandler.handleEvent(message)
      }

      RECALL_UPDATED_EVENT_TYPE -> {
        recallUpdatedHandler.handleEvent(message)
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
