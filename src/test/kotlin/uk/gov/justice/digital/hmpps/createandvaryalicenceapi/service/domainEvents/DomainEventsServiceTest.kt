package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

class DomainEventsServiceTest {
  private val outboundEventsPublisher = mock<OutboundEventsPublisher>()
  private val hmppsQueueServiceMock = mock<HmppsQueueService>()
  private val mockHmppsTopic = mock<HmppsTopic>()
  private val snsClient = mock<SnsAsyncClient>()

  private val domainEventsService = DomainEventsService(
    outboundEventsPublisher,
  )

  @Test
  fun `creates and publishes a domain event`() {
    val licenceId = "1"
    val crn = "crn"
    val nomsNumber = "nomsNumber"

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

    whenever(hmppsQueueServiceMock.findByTopicId("domainevents")).thenReturn(mockHmppsTopic)
    whenever(mockHmppsTopic.arn).thenReturn("arn:aws:sns:eu-west-2:000000000000:domainevents-topic")
    whenever(mockHmppsTopic.snsClient).thenReturn(snsClient)
    whenever(outboundEventsPublisher.createDomainEvent(LicenceDomainEventType.LICENCE_ACTIVATED, licenceId, crn, nomsNumber)).thenReturn(domainEvent)

    domainEventsService.recordDomainEvent(
      LicenceDomainEventType.LICENCE_ACTIVATED,
      licenceId,
      crn,
      nomsNumber,
    )

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(domainEvent, licenceId)
  }
}
