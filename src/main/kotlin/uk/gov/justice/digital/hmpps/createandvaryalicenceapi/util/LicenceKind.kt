package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.TimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.VariationLicence
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
  val clazz: () -> Class<out Licence>,
) {
  PRRD(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.PRRD_LICENCE_ACTIVATED },
    { LicenceDomainEventType.PRRD_LICENCE_INACTIVATED },
    { PrrdLicence::class.java },
  ),

  CRD(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
    { CrdLicence::class.java },
  ),

  VARIATION(
    { VARIATION_IN_PROGRESS },
    { error("Variation licences are only copied from existing licences") },
    { LicenceEventType.VARIATION_CREATED },
    { LicenceEventType.VARIATION_SUBMITTED },
    { LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED },
    { LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED },
    { VariationLicence::class.java },
  ),

  HARD_STOP(
    { IN_PROGRESS },
    { LicenceEventType.HARD_STOP_CREATED },
    { error("Hard stop licences can not be copied") },
    { LicenceEventType.HARD_STOP_SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
    { HardStopLicence::class.java },
  ),

  HDC(
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.HDC_LICENCE_ACTIVATED },
    { LicenceDomainEventType.HDC_LICENCE_INACTIVATED },
    { HdcLicence::class.java },

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
    { HdcVariationLicence::class.java },
  ) {
    override fun isHdc() = true
  },

  TIME_SERVED(
    { IN_PROGRESS },
    { LicenceEventType.TIME_SERVED_CREATED },
    @TimeServedConsiderations("Time served licences can not be copied?") { error("Time served licences can not be copied") },
    { LicenceEventType.TIME_SERVED_SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
    { TimeServedLicence::class.java },
  ),
  ;

  open fun isHdc(): Boolean = false
}
