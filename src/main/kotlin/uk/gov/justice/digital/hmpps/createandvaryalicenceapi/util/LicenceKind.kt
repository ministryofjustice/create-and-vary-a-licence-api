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
  val clazz: Class<out Licence>,
  val initialStatus: () -> LicenceStatus,
  val creationEventType: () -> LicenceEventType,
  val copyEventType: () -> LicenceEventType,
  val submittedEventType: () -> LicenceEventType,
  val activatedDomainEventType: () -> LicenceDomainEventType,
  val inactivatedDomainEventType: () -> LicenceDomainEventType,

) {
  PRRD(
    PrrdLicence::class.java,
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.PRRD_LICENCE_ACTIVATED },
    { LicenceDomainEventType.PRRD_LICENCE_INACTIVATED },

  ),

  CRD(
    CrdLicence::class.java,
    { IN_PROGRESS },
    { LicenceEventType.CREATED },
    { LicenceEventType.VERSION_CREATED },
    { LicenceEventType.SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
  ),

  VARIATION(
    VariationLicence::class.java,
    { VARIATION_IN_PROGRESS },
    { error("Variation licences are only copied from existing licences") },
    { LicenceEventType.VARIATION_CREATED },
    { LicenceEventType.VARIATION_SUBMITTED },
    { LicenceDomainEventType.LICENCE_VARIATION_ACTIVATED },
    { LicenceDomainEventType.LICENCE_VARIATION_INACTIVATED },
  ),

  HARD_STOP(
    HardStopLicence::class.java,
    { IN_PROGRESS },
    { LicenceEventType.HARD_STOP_CREATED },
    { error("Hard stop licences can not be copied") },
    { LicenceEventType.HARD_STOP_SUBMITTED },
    { LicenceDomainEventType.LICENCE_ACTIVATED },
    { LicenceDomainEventType.LICENCE_INACTIVATED },
  ),

  HDC(
    HdcLicence::class.java,
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
    HdcVariationLicence::class.java,
    { VARIATION_IN_PROGRESS },
    { error("HDC variation licences are only copied from existing HDC licences") },
    { LicenceEventType.VARIATION_CREATED },
    { LicenceEventType.VARIATION_SUBMITTED },
    { LicenceDomainEventType.HDC_LICENCE_VARIATION_ACTIVATED },
    { LicenceDomainEventType.HDC_LICENCE_VARIATION_INACTIVATED },
  ) {
    override fun isHdc() = true
  },

  TIME_SERVED(
    TimeServedLicence::class.java,
    { IN_PROGRESS },
    { LicenceEventType.TIME_SERVED_CREATED },
    { error("Time served licences cannot be copied") },
    { LicenceEventType.TIME_SERVED_SUBMITTED },
    { LicenceDomainEventType.TIME_SERVED_LICENCE_ACTIVATED },
    { LicenceDomainEventType.TIME_SERVED_LICENCE_INACTIVATED },
  ),
  ;

  open fun isHdc(): Boolean = false
}
