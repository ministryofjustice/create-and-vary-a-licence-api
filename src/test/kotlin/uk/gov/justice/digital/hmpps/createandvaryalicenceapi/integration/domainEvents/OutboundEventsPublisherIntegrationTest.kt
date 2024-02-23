package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class OutboundEventsPublisherIntegrationTest : IntegrationTestBase() {

  @Test
  fun `Outbound licence activated event is published to domain event topic`() {
    val sentEvent = DomainEventsService.HMPPSDomainEvent(
      DomainEventsService.LicenceDomainEventType.LICENCE_ACTIVATED.value,
      DomainEventsService.AdditionalInformation("1"),
      "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
      1,
      "2023-12-05T00:00:00Z",
      "Licence activated for 1",
      DomainEventsService.PersonReference(
        listOf(
          DomainEventsService.Identifiers("CRN", "A123456"),
          DomainEventsService.Identifiers("NOMS", "A1234BC"),
        ),
      ),
    )

    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(mapper.writeValueAsString(sentEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(sentEvent.eventType).build(),
          ),
        )
        .build(),
    ).get()

    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it != 0 }

    val (receivedMessage, _, _) = mapper.readValue(domainEventsSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(domainEventsQueueUrl).build()).get().messages()[0].body(), Message::class.java)
    val receivedEvent = mapper.readValue(receivedMessage, DomainEventsService.HMPPSDomainEvent::class.java)

    assertThat(receivedEvent.eventType).isEqualTo(DomainEventsService.LicenceDomainEventType.LICENCE_ACTIVATED.value)
    assertThat(receivedEvent.description).isEqualTo("Licence activated for 1")
  }
}
