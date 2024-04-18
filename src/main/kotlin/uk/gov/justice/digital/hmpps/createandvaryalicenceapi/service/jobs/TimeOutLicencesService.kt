package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.time.Clock
import java.time.LocalDate

@Service
class TimeOutLicencesService(
  @Value("\${hardstop.enabled}") private val hardStopEnabled: Boolean,
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
  private val workingDaysService: WorkingDaysService,
  private val clock: Clock,
  private val notifyService: NotifyService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun timeOutLicences() {
    log.info("Job to runTimeOutLicencesService started")
    val jobExecutionDate = LocalDate.now(clock)
    if (workingDaysService.isNonWorkingDay(jobExecutionDate)) {
      return
    }
    val licencesToTimeOut = licenceRepository.getAllLicencesToTimeOut().filter {
      releaseDateService.isInHardStopPeriod(it)
    }
    if (licencesToTimeOut.isEmpty()) {
      log.info("Job to runTimeOutLicencesService has no licences to time out")
      return
    }
    log.info("TimeOutLicencesServiceJob is updating status TIMED_OUT on ${licencesToTimeOut.size} licences")
    updateLicencesStatus(licencesToTimeOut)
    log.info("TimeOutLicencesServiceJob updated status TIMED_OUT on ${licencesToTimeOut.size} licences")
  }

  private fun updateLicencesStatus(licences: List<CrdLicence>, reason: String? = null) {
    val timeOutLicences = licences.map { it.timeOut() }

    licenceRepository.saveAllAndFlush(timeOutLicences)

    timeOutLicences.map { licence ->
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = licence.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "${reason ?: "Licence automatically timed out"} for ${licence.forename} ${licence.surname}",
          detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
        ),
      )

      licenceEventRepository.saveAndFlush(
        LicenceEvent(
          licenceId = licence.id,
          eventType = LicenceEventType.TIMED_OUT,
          username = "SYSTEM",
          forenames = "SYSTEM",
          surname = "SYSTEM",
          eventDescription = "${reason ?: "Licence automatically timed out"} for ${licence.forename} ${licence.surname}",
        ),
      )

      // previously approved now edited licence that is timed out
      if (hardStopEnabled && licence.versionOfId != null) {
        val com = licence.responsibleCom
        if (com != null) {
          notifyService.sendEditedLicenceTimedOutEmail(
            com.email,
            "${com.firstName} ${com.lastName}",
            licence.forename!!,
            licence.surname!!,
            licence.crn,
            licence.licenceStartDate,
            licence.id.toString(),
          )
        }
      }
    }
  }
}
