package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class OutboundEventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  companion object {
    const val DOMAIN_TOPIC_ID = "domainevents"
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(DOMAIN_TOPIC_ID)
      ?: throw NoSuchElementException("Topic with name $DOMAIN_TOPIC_ID doesn't exist")
  }
  private val domainEventsTopicClient by lazy { domainEventsTopic.snsClient }

  fun publishDomainEvent(event: HMPPSDomainEvent, licenceId: String) {
    val eventType = event.eventType
    log.debug("Event {} for Licence ID {}", eventType, licenceId)
    domainEventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopic.arn)
        .message(objectMapper.writeValueAsString(event))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(event.eventType).build(),
          ),
        )
        .build()
        .also { log.info("Published event $eventType to outbound topic") },
    )
  }
}
