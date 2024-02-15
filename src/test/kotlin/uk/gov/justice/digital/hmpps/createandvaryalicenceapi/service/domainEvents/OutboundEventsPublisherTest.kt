package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.AdditionalInformation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.PersonReference
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

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
      val licenceId = "1"
      val crn = "crn"
      val nomsNumber = "nomsNumber"

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val domainEvent = HMPPSDomainEvent(
        LicenceDomainEventType.LICENCE_ACTIVATED.value,
        AdditionalInformation(licenceId),
        "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
        1,
        "2023-12-05T00:00:00Z",
        "Licence activated for 1",
        PersonReference(
          listOf(
            Identifiers("CRN", crn),
            Identifiers("NOMS", nomsNumber),
          ),
        ),
      )

      outboundEventsPublisher.publishDomainEvent(domainEvent)

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
                    "value": "crn"
                  },
                  {
                    "type": "NOMS",
                    "value": "nomsNumber"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }

    @Test
    fun `publishes licence variation activated event to SNS`() {
      val licenceId = "1"
      val crn = "crn"
      val nomsNumber = "nomsNumber"

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val payload = HMPPSDomainEvent(
        LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value,
        AdditionalInformation(licenceId),
        "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
        1,
        "2023-12-05T00:00:00Z",
        "Licence activated for 1",
        PersonReference(
          listOf(
            Identifiers("CRN", crn),
            Identifiers("NOMS", nomsNumber),
          ),
        ),
      )

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
                    "value": "crn"
                  },
                  {
                    "type": "NOMS",
                    "value": "nomsNumber"
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
      val licenceId = "1"
      val crn = "crn"
      val nomsNumber = "nomsNumber"

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val payload = HMPPSDomainEvent(
        LicenceDomainEventType.LICENCE_INACTIVATED.value,
        AdditionalInformation(licenceId),
        "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
        1,
        "2023-12-05T00:00:00Z",
        "Licence activated for 1",
        PersonReference(
          listOf(
            Identifiers("CRN", crn),
            Identifiers("NOMS", nomsNumber),
          ),
        ),
      )

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
              "description": "Licence activated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "crn"
                  },
                  {
                    "type": "NOMS",
                    "value": "nomsNumber"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }

    @Test
    fun `publishes licence variation inactivated event to SNS`() {
      val licenceId = "1"
      val crn = "crn"
      val nomsNumber = "nomsNumber"

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val payload = HMPPSDomainEvent(
        LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value,
        AdditionalInformation(licenceId),
        "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
        1,
        "2023-12-05T00:00:00Z",
        "Licence activated for 1",
        PersonReference(
          listOf(
            Identifiers("CRN", crn),
            Identifiers("NOMS", nomsNumber),
          ),
        ),
      )

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
              "description": "Licence activated for 1",
              "personReference": {
                "identifiers": [
                  {
                    "type": "CRN",
                    "value": "crn"
                  },
                  {
                    "type": "NOMS",
                    "value": "nomsNumber"
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
      val licenceId = "1"
      val crn = null
      val nomsNumber = "nomsNumber"

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val payload = HMPPSDomainEvent(
        LicenceDomainEventType.LICENCE_ACTIVATED.value,
        AdditionalInformation(licenceId),
        "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
        1,
        "2023-12-05T00:00:00Z",
        "Licence activated for 1",
        PersonReference(
          listOf(
            Identifiers("CRN", crn),
            Identifiers("NOMS", nomsNumber),
          ),
        ),
      )

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
                    "value": "nomsNumber"
                  }
                ]
              }
            }
        """.trimIndent(),
      )
    }

    @Test
    fun `licence variation activated event publishes to SNS without NOMIS number`() {
      val licenceId = "1"
      val crn = "crn"
      val nomsNumber = null

      val requestCaptor = ArgumentCaptor.forClass(PublishRequest::class.java)

      whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
      whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
      whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)

      val payload = HMPPSDomainEvent(
        LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value,
        AdditionalInformation(licenceId),
        "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/1",
        1,
        "2023-12-05T00:00:00Z",
        "Licence activated for 1",
        PersonReference(
          listOf(
            Identifiers("CRN", crn),
            Identifiers("NOMS", nomsNumber),
          ),
        ),
      )

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
                    "value": "crn"
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
}
