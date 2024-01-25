package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.springframework.stereotype.Service

@Service
class DomainEventsService(
  private val outboundEventsPublisher: OutboundEventsPublisher,
) {
  fun recordDomainEvent(event: LicenceDomainEventType, licenceId: String, crn: String?, nomsNumber: String?) {
    val domainEvent = outboundEventsPublisher.createDomainEvent(event, licenceId, crn, nomsNumber)
    outboundEventsPublisher.publishDomainEvent(domainEvent, licenceId)
  }
}
