package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

const val COM_ALLOCATED_EVENT_TYPE = "person.community.manager.allocated"

@ConditionalOnProperty(name = ["domain.event.listener.enabled"], havingValue = "true", matchIfMissing = false)
@Service
class DomainEventListener(
  private val comAllocatedHandler: ComAllocatedHandler,
  private val objectMapper: ObjectMapper,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("domaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(
    rawMessage: String,
  ) {
    val message: Message = objectMapper.readValue(rawMessage)
    val hmppsMessage: HMPPSDomainEvent = objectMapper.readValue(message.message)

    when (hmppsMessage.eventType) {
      COM_ALLOCATED_EVENT_TYPE -> {
        comAllocatedHandler.processComAllocation(hmppsMessage.personReference.crn())
      }

      else -> {
        log.warn("Ignoring message with type ${hmppsMessage.eventType}")
      }
    }
  }
}
