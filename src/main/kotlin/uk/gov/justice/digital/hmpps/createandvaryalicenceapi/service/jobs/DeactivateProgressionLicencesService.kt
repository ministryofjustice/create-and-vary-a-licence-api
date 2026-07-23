package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isOnOrBefore
import java.time.LocalDate
import kotlin.collections.forEach

@Service
class DeactivateProgressionLicencesService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val domainEventsService: DomainEventsService,
  private val telemetryService: TelemetryService,
  private val notifyService: NotifyService,

  @param:Value("\${progression.model.policy-start-date}")
  @param:DateTimeFormat(pattern = "yyyy-MM-dd")
  private val policyV4StartDate: LocalDate,

  @param:Value("\${progression.model.notification-window-end-date}")
  @param:DateTimeFormat(pattern = "yyyy-MM-dd")
  private val notificationWindowEndDate: LocalDate,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun deactivateLicences() {
    log.info("Job deactivateProgressionLicences started")
    val licencesToDeactivate = licenceRepository.getLicencesForProgressionDeactivation(policyV4StartDate)
    if (licencesToDeactivate.isEmpty()) {
      log.info("Job deactivateProgressionLicences has no licences to deactivate")
      return
    }
    log.info("deactivateProgressionLicences is updating status INACTIVE on ${licencesToDeactivate.size} licences")
    updateLicencesStatus(licencesToDeactivate)
    log.info("deactivateProgressionLicences updated status to INACTIVE on ${licencesToDeactivate.size} licences")
  }

  private fun updateLicencesStatus(licences: List<Licence>) {
    licences.forEach { it.deactivate() }

    licenceRepository.saveAllAndFlush(licences)

    licences.forEach { licence ->
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = licence.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = SYSTEM_EVENT,
          summary = "Licence automatically deactivated as it is a progression licence on an older policy version for ${licence.forename} ${licence.surname}",
          detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        ),
      )

      licenceEventRepository.saveAndFlush(
        LicenceEvent(
          licenceId = licence.id,
          eventType = LicenceEventType.INACTIVE,
          username = "SYSTEM",
          forenames = "SYSTEM",
          surname = "SYSTEM",
          eventDescription = "Licence automatically deactivated as it is a progression licence on an older policy version for ${licence.forename} ${licence.surname}",
        ),
      )
      domainEventsService.recordDomainEvent(licence, LicenceStatus.INACTIVE)

      // query relies on Licence Start Date being past given date, so nulls should not be returned
      if (licence.licenceStartDate!!.isOnOrBefore(notificationWindowEndDate)) {
        if (licence.responsibleCom == null) {
          log.info("Unable to notify COM of progression licence deactivation as licence has no responsible com")
          return@forEach
        }

        notifyService.sendLicenceDeactivatedForProgressionEmail(
          emailAddress = licence.responsibleCom?.email,
          crn = licence.crn!!,
          comFirstName = licence.responsibleCom!!.firstName!!,
          comLastName = licence.responsibleCom!!.lastName!!,
          pipFirstName = licence.forename!!,
          pipLastName = licence.surname!!,
        )
      }
    }
    telemetryService.recordDeactivateProgressionLicencesJobEvent(licences.size)
  }
}
