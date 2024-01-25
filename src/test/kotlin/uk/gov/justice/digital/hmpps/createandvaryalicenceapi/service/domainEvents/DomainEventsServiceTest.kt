package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DomainEventsServiceTest {
  private val outboundEventsPublisher = mock<OutboundEventsPublisher>()
  private val clock: Clock = Clock.fixed(Instant.parse("2023-12-05T00:00:00Z"), ZoneId.systemDefault())

  private val domainEventsService = DomainEventsService(
    outboundEventsPublisher,
    clock,
  )

  @Test
  fun `creates and publishes a domain event`() {
    val licenceId = "1"
    val crn = "crn"
    val nomsNumber = "nomsNumber"

    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(
      LicenceDomainEventType.LICENCE_ACTIVATED,
      licenceId,
      crn,
      nomsNumber,
    )

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_ACTIVATED.value)
  }
}
