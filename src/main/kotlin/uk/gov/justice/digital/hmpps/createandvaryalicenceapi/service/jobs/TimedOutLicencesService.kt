package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import java.time.LocalDate

@Service
class TimedOutLicencesService(
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
  private val licenceService: LicenceService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun timedOutLicencesJob() {
    log.info("Job to runTimedOutLicencesService started")
    val timeOutDate = releaseDateService.getCutOffDateForLicenceTimeOut(LocalDate.now())
    val licencesToBeTimedOut = licenceRepository.getAllLicencesToBeTimedOut(timeOutDate)
    if (licencesToBeTimedOut.isEmpty()) {
      return
    }
    licenceService.timedOutLicences(licencesToBeTimedOut)
    log.info("TimedOutLicencesServiceJob updated status TIME_OUT on ${licencesToBeTimedOut.size} licences")
  }
}
