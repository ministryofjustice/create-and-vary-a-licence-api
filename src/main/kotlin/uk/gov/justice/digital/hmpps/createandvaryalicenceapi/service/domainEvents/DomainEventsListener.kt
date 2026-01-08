package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

const val COM_ALLOCATED_EVENT_TYPE = "person.community.manager.allocated"
const val PRISONER_UPDATED_EVENT_TYPE = "prisoner-offender-search.prisoner.updated"

@ConditionalOnProperty(name = ["domain.event.listener.disabled"], havingValue = "false", matchIfMissing = true)
@Service
class DomainEventListener(
  private val comAllocatedHandler: ComAllocatedHandler,
  private val prisonerUpdatedHandler: PrisonerUpdatedHandler,
  private val objectMapper: ObjectMapper,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    val (message, _, messageAttributes) = objectMapper.readValue<Message>(rawMessage)

    when (val eventType = messageAttributes.eventType.value) {
      COM_ALLOCATED_EVENT_TYPE -> {
        comAllocatedHandler.handleEvent(message)
      }

      PRISONER_UPDATED_EVENT_TYPE -> {
        prisonerUpdatedHandler.handleEvent(message)
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
