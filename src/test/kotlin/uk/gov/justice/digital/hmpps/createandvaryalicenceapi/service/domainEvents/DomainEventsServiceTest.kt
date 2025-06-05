package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.context.ApplicationEventPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DomainEventsServiceTest {
  private val clock: Clock = Clock.fixed(Instant.parse("2023-12-05T00:00:00Z"), ZoneId.systemDefault())
  private val applicationEventPublisher = mock<ApplicationEventPublisher>()

  private val domainEventsService = DomainEventsService(
    "http://test123",
    clock,
    applicationEventPublisher,
  )

  @Test
  fun `create and publishes a domain event for an active CRD licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createCrdLicence(), LicenceStatus.ACTIVE)

    verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_ACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence activated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an inactive CRD licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createCrdLicence(), LicenceStatus.INACTIVE)

    verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence inactivated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an active VARIATION licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createVariationLicence(), LicenceStatus.ACTIVE)

    verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence activated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an inactive VARIATION licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createVariationLicence(), LicenceStatus.INACTIVE)

    verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence inactivated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an active HARD STOP licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createHardStopLicence(), LicenceStatus.ACTIVE)

    verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_ACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence activated for Licence ID 1")
  }

  @Test
  fun `create and publishes a domain event for an inactive HARD STOP licence`() {
    val eventCaptor = argumentCaptor<HMPPSDomainEvent>()

    domainEventsService.recordDomainEvent(TestData.createHardStopLicence(), LicenceStatus.INACTIVE)

    verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture())

    assertThat(eventCaptor.firstValue.eventType).isEqualTo(LicenceDomainEventType.LICENCE_INACTIVATED.value)

    assertThat(eventCaptor.firstValue.description).isEqualTo("Licence inactivated for Licence ID 1")
  }

  @Test
  fun `does not create and publishes a domain event when status is not ACTIVE or INACTIVE`() {
    domainEventsService.recordDomainEvent(TestData.createCrdLicence(), LicenceStatus.SUBMITTED)

    verifyNoInteractions(applicationEventPublisher)
  }
}
