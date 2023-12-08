package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TimeOutLicencesService(
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val auditEventRepository: AuditEventRepository,
  private val licenceEventRepository: LicenceEventRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun timeOutLicencesJob() {
    log.info("Job to runTimedOutLicencesService started")
    val timeOutDate = releaseDateService.getCutOffDateForLicenceTimeOut(LocalDate.now())
    val licencesToBeTimedOut = licenceRepository.getAllLicencesToBeTimedOut(timeOutDate)
    if (licencesToBeTimedOut.isEmpty()) {
      return
    }
    updateLicencesStatus(licencesToBeTimedOut)
    log.info("TimedOutLicencesServiceJob updated status TIME_OUT on ${licencesToBeTimedOut.size} licences")
  }

  @Transactional
  fun updateLicencesStatus(licences: List<Licence>, reason: String? = null) {
    val timedOutLicences = licences.map {
      it.copy(
        statusCode = LicenceStatus.TIME_OUT,
        dateLastUpdated = LocalDateTime.now(),
        updatedByUsername = "SYSTEM",
      )
    }
    if (timedOutLicences.isNotEmpty()) {
      licenceRepository.saveAllAndFlush(timedOutLicences)

      timedOutLicences.map { licence ->
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
            eventType = LicenceEventType.TIME_OUT,
            username = "SYSTEM",
            forenames = "SYSTEM",
            surname = "SYSTEM",
            eventDescription = "${reason ?: "Licence automatically timed out"} for ${licence.forename} ${licence.surname}",
          ),
        )
      }
    }
  }
}
