package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventsService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Service
class DeactivateHdcLicencesService(
  private val licenceRepository: LicenceRepository,
  private val hdcLicenceRepository: HdcLicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val domainEventsService: DomainEventsService,
  private val telemetryService: TelemetryService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun runJob() {
    log.info("Running job to deactivate HDC licences")
    val licencesToDeactivate = hdcLicenceRepository.getDraftLicencesIneligibleForHdcRelease()
    if (licencesToDeactivate.isEmpty()) {
      log.info("There are no HDC licences to deactivate")
      return
    }
    log.info(
      "Found {} prisoners who have draft HDC licences that are now ineligible for HDC release",
      licencesToDeactivate.size,
    )
    updateLicencesStatus(licencesToDeactivate)
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
          summary = "HDC licence automatically deactivated as now ineligible for HDC release for ${licence.forename} ${licence.surname}",
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
          eventDescription = "HDC licence automatically deactivated as now ineligible for HDC release for ${licence.forename} ${licence.surname}",
        ),
      )
      domainEventsService.recordDomainEvent(licence, LicenceStatus.INACTIVE)
    }
    telemetryService.recordDeactivateHdcLicencesJobEvent(licences.size)
  }
}
