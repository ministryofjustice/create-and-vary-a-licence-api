package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import software.amazon.awssdk.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.mockito.kotlin.mock
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.sqs.HmppsTopic
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDomainEventType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DomainEventsServiceTest {
  private val hmppsQueueServiceMock = mock<HmppsQueueService>()

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
  inner class ApplicationSubmitted {
    @Nested
    inner class LicenceActivatedDomainEvent {
      @Test
      fun `publishes event to SNS`() {
        val licenceId = "1"
        val crn = "crn"
        var nomsNumber = "nomsNumber"

        val mockHmppsTopic = mock<HmppsTopic>()

        whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
        whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
        whenever(mockHmppsTopic.snsClient.publish(any())).thenReturn(PublishRequest.builder())

        domainEventsService.publishDomainEvent(
          LicenceDomainEventType.LICENCE_ACTIVATED,
          licenceId,
          crn,
          nomsNumber)

        verify(mockHmppsTopic.snsClient.publish) {
          mockHmppsTopic.snsClient.publish(
            match {
              val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

              deserializedMessage.eventType == "create-and-vary-a-licence.licence.activated" &&
                deserializedMessage.version == 1 &&
                deserializedMessage.description == "Licence activated for $licenceId" &&
                deserializedMessage.detailUrl == "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId"
            },
          )
        }
      }
    }
  }
}
