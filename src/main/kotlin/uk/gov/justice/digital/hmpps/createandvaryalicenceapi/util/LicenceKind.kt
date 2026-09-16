package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService.LicenceDomainEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS

enum class LicenceKind(
  val initialStatus: () -> LicenceStatus,
  val creationEventType: () -> LicenceEventType,
  val copyEventType: () -> LicenceEventType,
  val submittedEventType: () -> LicenceEventType,
  val activatedDomainEventType: () -> LicenceDomainEventType,
  val inactivatedDomainEventType: () -> LicenceDomainEventType,

) {
  PRRD(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.PRRD_LICENCE_ACTIVATED },
    { LicenceDomainEventType.PRRD_LICENCE_INACTIVATED },
  ),

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
  ) {
    override fun isVariation() = true
  },

  HARD_STOP(
    { IN_PROGRESS },
    { LicenceEventType.HARD_STOP_CREATED },
    { error("Hard stop licences can not be copied") },
    { LicenceEventType.HARD_STOP_SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
  ) {
    override fun isCreatedByPrison() = true
  },

  HDC(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.HDC_LICENCE_ACTIVATED },
    { LicenceDomainEventType.HDC_LICENCE_INACTIVATED },

  ) {
    override fun isHdc() = true
  },

  HDC_VARIATION(
    { VARIATION_IN_PROGRESS },
    { error("HDC variation licences are only copied from existing HDC licences") },
    { LicenceEventType.VARIATION_CREATED },
    { LicenceEventType.VARIATION_SUBMITTED },
    { LicenceDomainEventType.HDC_LICENCE_VARIATION_ACTIVATED },
    { LicenceDomainEventType.HDC_LICENCE_VARIATION_INACTIVATED },
  ) {
    override fun isHdc() = true
    override fun isVariation() = true
  },

  TIME_SERVED(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { error("Time served licences cannot be copied") },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.TIME_SERVED_LICENCE_ACTIVATED },
    { LicenceDomainEventType.TIME_SERVED_LICENCE_INACTIVATED },
  ) {
    override fun isCreatedByPrison() = true
  },
  ;

  open fun isCreatedByPrison(): Boolean = false
  open fun isVariation(): Boolean = false
  open fun isHdc(): Boolean = false
}
