package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.concurrent.TimeUnit

@Service
class OutboundEventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  companion object {
    const val DOMAIN_TOPIC_ID = "domainevents"
    const val FUTURE_TIMEOUT = 10000L
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(DOMAIN_TOPIC_ID)
      ?: throw NoSuchElementException("Topic with name $DOMAIN_TOPIC_ID doesn't exist")
  }
  private val domainEventsTopicClient by lazy { domainEventsTopic.snsClient }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun publishDomainEvent(event: HMPPSDomainEvent) {
    val eventType = event.eventType
    domainEventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopic.arn)
        .message(objectMapper.writeValueAsString(event))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(event.eventType).build(),
          ),
        )
        .build(),
    ).get(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS)
    log.info("Event {} raised for licence ID {}", eventType, event.additionalInformation?.licenceId)
  }
}
