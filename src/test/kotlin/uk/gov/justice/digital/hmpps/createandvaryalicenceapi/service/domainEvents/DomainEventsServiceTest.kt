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
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DomainEventsServiceTest {
  private val hmppsQueueServiceMock = mock<HmppsQueueService>()
  private val mockHmppsTopic = mock<HmppsTopic>()
  private val snsClient = mock<SnsAsyncClient>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val clock: Clock = Clock.fixed(Instant.parse("2023-12-05T00:00:00Z"), ZoneId.systemDefault())

  private val domainEventsService = DomainEventsService(
    hmppsQueueService = hmppsQueueServiceMock,
    objectMapper = objectMapper,
    clock = clock,
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

      domainEventsService.publishDomainEvent(
        LicenceDomainEventType.LICENCE_ACTIVATED,
        licenceId,
        crn,
        nomsNumber,
      )

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

      domainEventsService.publishDomainEvent(
        LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED,
        licenceId,
        crn,
        nomsNumber,
      )

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

      domainEventsService.publishDomainEvent(
        LicenceDomainEventType.LICENCE_INACTIVATED,
        licenceId,
        crn,
        nomsNumber,
      )

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

      domainEventsService.publishDomainEvent(
        LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED,
        licenceId,
        crn,
        nomsNumber,
      )

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
}
