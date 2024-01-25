package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class DomainEventsService(
  private val outboundEventsPublisher: OutboundEventsPublisher,
  private val clock: Clock,
) {
  fun recordDomainEvent(event: LicenceDomainEventType, licenceId: String, crn: String?, nomsNumber: String?) {
    outboundEventsPublisher
      .publishDomainEvent(
        createDomainEvent(event, licenceId, crn, nomsNumber),
        licenceId,
      )
  }

  private fun createDomainEvent(event: LicenceDomainEventType, licenceId: String, crn: String?, nomsNumber: String?): HMPPSDomainEvent {
    val additionalInformation = AdditionalInformation(licenceId)
    val personReferenceIdentifiers = PersonReference(
      listOf(Identifiers("CRN", crn), Identifiers("NOMS", nomsNumber)),
    )
    val eventType = event.value
    val occurredAt = LocalDateTime.now(clock)
    val description = "Licence activated for $licenceId"
    return HMPPSDomainEvent(
      eventType,
      additionalInformation,
      "https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk/public/licences/id/$licenceId",
      1,
      occurredAt.toOffsetDateFormat(),
      description,
      personReferenceIdentifiers,
    )
  }

  data class AdditionalInformation(
    val licenceId: String,
  )

  data class PersonReference(
    val identifiers: List<Identifiers>,
  )

  data class Identifiers(
    val type: String,
    val value: String?,
  )

  data class HMPPSDomainEvent(
    val eventType: String? = null,
    val additionalInformation: AdditionalInformation?,
    val detailUrl: String,
    val version: Int,
    val occurredAt: String,
    val description: String,
    val personReference: PersonReference,
  )

  enum class LicenceDomainEventType(val value: String) {
    LICENCE_ACTIVATED("create-and-vary-a-licence.licence.activated"),
    LICENCE_VARIATION_ACTIVATED("create-and-vary-a-licence.variation.activated"),
    LICENCE_INACTIVATED("create-and-vary-a-licence.licence.inactivated"),
    LICENCE_VARIATION_INACTIVATED("create-and-vary-a-licence.variation.inactivated"),
  }

  fun LocalDateTime.toOffsetDateFormat(): String =
    atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
