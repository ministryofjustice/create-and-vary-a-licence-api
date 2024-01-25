package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class DomainEventsService(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val clock: Clock,
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

  fun publishDomainEvent(
    event: LicenceDomainEventType,
    licenceId: String,
    crn: String,
    nomsNumber: String,
  ) {
    val additionalInformation = AdditionalInformation(licenceId)
    val personReferenceIdentifiers = PersonReference(
      listOf(Identifiers("CRN", crn), Identifiers("NOMS", nomsNumber)),
    )
    val eventType = event.value
    val occurredAt = LocalDateTime.now(clock)
    val description = "Licence activated for $licenceId"
    val payload = HMPPSDomainEvent(
      eventType,
      additionalInformation,
      "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId",
      1,
      occurredAt.toOffsetDateFormat(),
      description,
      personReferenceIdentifiers,
    )

    log.debug("Event {} for id {}", payload.eventType, licenceId)
    domainEventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopic.arn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()
        .also { log.info("Published event $eventType to outbound topic") },
    )
  }
}

data class AdditionalInformation(
  val licenceId: String,
)

data class PersonReference(
  val identifiers: List<Identifiers>,
)

data class Identifiers(
  val type: String,
  val value: String,
)

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation?,
  val detailUrl: String,
  val version: Int,
  val occurredAt: String,
  val description: String,
  val personReference: PersonReference,
)

enum class LicenceDomainEventType(val value: String) {
  LICENCE_ACTIVATED("create-and-vary-a-licence.licence.activated"),
  LICENCE_VARIATION_ACTIVATED("create-and-vary-a-licence.variation.activated"),
  LICENCE_INACTIVATED("create-and-vary-a-licence.licence.inactivated"),
  LICENCE_VARIATION_INACTIVATED("create-and-vary-a-licence.variation.inactivated"),
}

fun LocalDateTime.toOffsetDateFormat(): String =
  atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
