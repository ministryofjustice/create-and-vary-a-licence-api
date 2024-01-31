package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DomainEventsServiceTest {
  private val outboundEventsPublisher = mock<OutboundEventsPublisher>()
  private val clock: Clock = Clock.fixed(Instant.parse("2023-12-05T00:00:00Z"), ZoneId.systemDefault())

  private val domainEventsService = DomainEventsService(
    "http://test123",
    outboundEventsPublisher,
    clock,
  )

  @Test
  fun `create and publishes a domain event for an active CRD licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createCrdLicence(), LicenceStatus.ACTIVE)

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_ACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence activated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an inactive CRD licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createCrdLicence(), LicenceStatus.INACTIVE)

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence inactivated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an active VARIATION licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createVariationLicence(), LicenceStatus.ACTIVE)

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence activated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an inactive VARIATION licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createVariationLicence(), LicenceStatus.INACTIVE)

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence inactivated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an active HARD STOP licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createHardStopLicence(), LicenceStatus.ACTIVE)

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_ACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence activated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an inactive HARD STOP licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createHardStopLicence(), LicenceStatus.INACTIVE)

    verify(outboundEventsPublisher, times(1)).publishDomainEvent(eventCaptor.capture(), any())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence inactivated for Licence ID 1")
  }

  @Test
  fun `does not create and publishes a domain event when status is not ACTIVE or INACTIVE`() {
    domainEventsService.recordDomainEvent(TestData.createCrdLicence(), LicenceStatus.SUBMITTED)

    verifyNoInteractions(outboundEventsPublisher)
  }
}
