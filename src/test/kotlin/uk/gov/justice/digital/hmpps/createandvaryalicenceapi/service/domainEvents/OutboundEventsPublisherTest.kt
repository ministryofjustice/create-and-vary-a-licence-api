package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.AdditionalInformation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.PersonReference
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.concurrent.CompletableFuture

class OutboundEventsPublisherTest {
  private val hmppsQueueServiceMock = mock<HmppsQueueService>()
  private val mockHmppsTopic = mock<HmppsTopic>()
  private val snsClient = mock<SnsAsyncClient>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val outboundEventsPublisher = OutboundEventsPublisher(
    hmppsQueueService = hmppsQueueServiceMock,
    objectMapper = objectMapper,
  )

  @Nested
  inner class LicenceActivatedDomainEvent {
    @Test
    fun `publishes licence activated event to SNS`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(aHMPPSDomainEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(aHMPPSDomainEvent.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      outboundEventsPublisher.publishDomainEvent(aHMPPSDomainEvent)

      verify(snsClient, times(1)).publish(requestCaptor.capture())

      assertThatJson(requestCaptor.value.message()).isEqualTo(
        """
            {
              "eventType": "create-and-vary-a-licence.licence.activated",
              "additionalInformation": {
                "licenceId": "1"
              },
              "detailUrl": "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
              "version": 1,
              "occurredAt": "2023-12-05T00:00:00Z",
              "description": "Licence activated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "A123456"
                  },
                  {
                    "type": "NOMS",
                    "value": "A1234BC"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }

    @Test
    fun `publishes licence variation activated event to SNS`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      val payload = aHMPPSDomainEvent.copy(
        eventType = LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value,
      )

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      outboundEventsPublisher.publishDomainEvent(payload)

      verify(snsClient, times(1)).publish(requestCaptor.capture())

      assertThatJson(requestCaptor.value.message()).isEqualTo(
        """
            {
              "eventType": "create-and-vary-a-licence.variation.activated",
              "additionalInformation": {
                "licenceId": "1"
              },
              "detailUrl": "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
              "version": 1,
              "occurredAt": "2023-12-05T00:00:00Z",
              "description": "Licence activated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "A123456"
                  },
                  {
                    "type": "NOMS",
                    "value": "A1234BC"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }
  }

  @Nested
  inner class LicenceInactivatedDomainEvent {
    @Test
    fun `publishes licence inactivated event to SNS`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      val payload = aHMPPSDomainEvent.copy(
        eventType = LicenceDomainEventType.LICENCE_INACTIVATED.value,
        description = "Licence inactivated for 1",
      )

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      outboundEventsPublisher.publishDomainEvent(payload)

      verify(snsClient, times(1)).publish(requestCaptor.capture())

      assertThatJson(requestCaptor.value.message()).isEqualTo(
        """
            {
              "eventType": "create-and-vary-a-licence.licence.inactivated",
              "additionalInformation": {
                "licenceId": "1"
              },
              "detailUrl": "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
              "version": 1,
              "occurredAt": "2023-12-05T00:00:00Z",
              "description": "Licence inactivated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "A123456"
                  },
                  {
                    "type": "NOMS",
                    "value": "A1234BC"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }

    @Test
    fun `publishes licence variation inactivated event to SNS`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      val payload = aHMPPSDomainEvent.copy(
        eventType = LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value,
        description = "Licence inactivated for 1",
      )

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      outboundEventsPublisher.publishDomainEvent(payload)

      verify(snsClient, times(1)).publish(requestCaptor.capture())

      assertThatJson(requestCaptor.value.message()).isEqualTo(
        """
            {
              "eventType": "create-and-vary-a-licence.variation.inactivated",
              "additionalInformation": {
                "licenceId": "1"
              },
              "detailUrl": "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
              "version": 1,
              "occurredAt": "2023-12-05T00:00:00Z",
              "description": "Licence inactivated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "A123456"
                  },
                  {
                    "type": "NOMS",
                    "value": "A1234BC"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }
  }

  @Nested
  inner class LicenceDomainEventWithoutIdentifiers {
    @Test
    fun `licence activated event publishes to SNS without CRN`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      val payload = aHMPPSDomainEvent.copy(
        personReference = PersonReference(
          listOf(
            Identifiers("CRN", null),
            Identifiers("NOMS", "A1234BC"),
          ),
        ),
      )

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      outboundEventsPublisher.publishDomainEvent(payload)

      verify(snsClient, times(1)).publish(requestCaptor.capture())

      assertThatJson(requestCaptor.value.message()).isEqualTo(
        """
            {
              "eventType": "create-and-vary-a-licence.licence.activated",
              "additionalInformation": {
                "licenceId": "1"
              },
              "detailUrl": "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
              "version": 1,
              "occurredAt": "2023-12-05T00:00:00Z",
              "description": "Licence activated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": null
                  },
                  {
                    "type": "NOMS",
                    "value": "A1234BC"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }

    @Test
    fun `licence variation activated event publishes to SNS without NOMIS number`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)
      val payload = aHMPPSDomainEvent.copy(
        eventType = LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value,
        personReference = PersonReference(
          listOf(
            Identifiers("CRN", "A123456"),
            Identifiers("NOMS", null),
          ),
        ),
      )
      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      outboundEventsPublisher.publishDomainEvent(payload)

      verify(snsClient, times(1)).publish(requestCaptor.capture())

      assertThatJson(requestCaptor.value.message()).isEqualTo(
        """
            {
              "eventType": "create-and-vary-a-licence.variation.activated",
              "additionalInformation": {
                "licenceId": "1"
              },
              "detailUrl": "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
              "version": 1,
              "occurredAt": "2023-12-05T00:00:00Z",
              "description": "Licence activated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "A123456"
                  },
                  {
                    "type": "NOMS",
                    "value": null
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }
  }

  @Nested
  inner class BuildEvent {

    @Test
    fun `build event completes successfully`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(aHMPPSDomainEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(aHMPPSDomainEvent.eventType)
              .build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.completedFuture(aPublishResponse))

      val publishedEvent = outboundEventsPublisher.buildDomainEvent(aHMPPSDomainEvent)

      assertThat(publishedEvent.isDone).isTrue()
    }

    @Test
    fun `failed future completes exceptionally when building an event`() {
      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn(anArn)
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val publishRequest = PublishRequest.builder()
        .topicArn(anArn)
        .message(objectMapper.writeValueAsString(aHMPPSDomainEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(aHMPPSDomainEvent.eventType).build(),
          ),
        )
        .build()

      whenever(snsClient.publish(publishRequest)).thenReturn(CompletableFuture.failedFuture(Exception("Exception")))

      val publishedEvent = outboundEventsPublisher.buildDomainEvent(aHMPPSDomainEvent)

      assertThat(publishedEvent.isCompletedExceptionally).isTrue()
    }
  }

  private companion object {
    val anArn = "arn:aws:sns:eu-west-2:000000000000:domainevents-topic"

    val aHMPPSDomainEvent = HMPPSDomainEvent(
      LicenceDomainEventType.LICENCE_ACTIVATED.value,
      AdditionalInformation("1"),
      "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
      1,
      "2023-12-05T00:00:00Z",
      "Licence activated for 1",
      PersonReference(
        listOf(
          Identifiers("CRN", "A123456"),
          Identifiers("NOMS", "A1234BC"),
        ),
      ),
    )

    val aPublishResponse = PublishResponse.builder()
      .messageId("a1bc-d2efg-hi3j-4567-k89l")
      .build()
  }
}
