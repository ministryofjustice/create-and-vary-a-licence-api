package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class DomainEventsService(
  @Value("\${self.api.link}") private val baseUrl: String,
  private val clock: Clock,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  fun recordDomainEvent(licence: Licence, licenceStatus: LicenceStatus) {
    when (licenceStatus) {
      LicenceStatus.ACTIVE -> {
        val domainEvent = createDomainEvent(
          licence.kind.activatedDomainEventType(),
          licence.id.toString(),
          licence.crn,
          licence.nomsId,
          "Licence activated for Licence ID ${licence.id}",
        )
        applicationEventPublisher.publishEvent(domainEvent)
      }

      LicenceStatus.INACTIVE -> {
        val domainEvent = createDomainEvent(
          licence.kind.inactivatedDomainEventType(),
          licence.id.toString(),
          licence.crn,
          licence.nomsId,
          "Licence inactivated for Licence ID ${licence.id}",
        )
        applicationEventPublisher.publishEvent(domainEvent)
      }

      else -> return
    }
  }

  private fun createDomainEvent(
    event: LicenceDomainEventType?,
    licenceId: String,
    crn: String?,
    nomsNumber: String?,
    description: String,
  ): HMPPSDomainEvent {
    val additionalInformation = AdditionalInformation(licenceId)
    val personReferenceIdentifiers = PersonReference(
      listOf(Identifiers("CRN", crn), Identifiers("NOMS", nomsNumber)),
    )
    val eventType = event?.value
    val occurredAt = LocalDateTime.now(clock)
    return HMPPSDomainEvent(
      eventType,
      additionalInformation,
      baseUrl.plus("/public/licences/id/$licenceId"),
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
    HDC_LICENCE_ACTIVATED("create-and-vary-a-licence.hdc-licence.activated"),
    LICENCE_VARIATION_ACTIVATED("create-and-vary-a-licence.variation.activated"),
    LICENCE_INACTIVATED("create-and-vary-a-licence.licence.inactivated"),
    HDC_LICENCE_INACTIVATED("create-and-vary-a-licence.hdc-licence.inactivated"),
    LICENCE_VARIATION_INACTIVATED("create-and-vary-a-licence.variation.inactivated"),
  }

  fun LocalDateTime.toOffsetDateFormat(): String = atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
