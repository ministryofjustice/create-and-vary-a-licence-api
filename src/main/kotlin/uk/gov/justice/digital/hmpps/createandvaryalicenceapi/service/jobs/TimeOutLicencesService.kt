package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CrdLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.Clock
import java.time.LocalDate

@Service
class TimeOutLicencesService(
  private val crdLicenceRepository: CrdLicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val workingDaysService: WorkingDaysService,
  private val clock: Clock,
  private val licenceService: LicenceService,
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
    val licencesToTimeOut = crdLicenceRepository.getAllLicencesToTimeOut().filter {
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

  private fun updateLicencesStatus(licences: List<CrdLicence>) {
    licences.map { licence -> licenceService.timeout(licence, "due to reaching hard stop") }
  }
}
