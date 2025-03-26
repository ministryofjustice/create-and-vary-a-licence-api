package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS

enum class LicenceKind(
  val initialStatus: () -> LicenceStatus,
  val creationEventType: () -> LicenceEventType,
  val copyEventType: () -> LicenceEventType,
  val submittedEventType: () -> LicenceEventType,
  val activatedDomainEventType: () -> LicenceDomainEventType? = { null },
  val inactivatedDomainEventType: () -> LicenceDomainEventType? = { null },
) {
  CRD(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
  ),

  VARIATION(
    { VARIATION_IN_PROGRESS },
    { error("Variation licences are only copied from existing licences") },
    { LicenceEventType.VARIATION_CREATED },
    { LicenceEventType.VARIATION_SUBMITTED },
    { LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED },
    { LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED },
  ),

  HARD_STOP(
    { IN_PROGRESS },
    { LicenceEventType.HARD_STOP_CREATED },
    { error("Hard stop licences can not be copied") },
    { LicenceEventType.HARD_STOP_SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
  ),

  HDC(
    { IN_PROGRESS },
    { LicenceEventType.HDC_CREATED },
    { LicenceEventType.HDC_VERSION_CREATED },
    { LicenceEventType.HDC_SUBMITTED },
    { LicenceDomainEventType.HDC_LICENCE_ACTIVATED },
    { LicenceDomainEventType.HDC_LICENCE_INACTIVATED },
  ),

  HDC_VARIATION(
    { VARIATION_IN_PROGRESS },
    { error("HDC variation licences are only copied from existing licences") },
    { LicenceEventType.HDC_VARIATION_CREATED },
    { LicenceEventType.HDC_VARIATION_SUBMITTED },
  ),
}
