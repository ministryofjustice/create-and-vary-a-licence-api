package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

@Service
class HardStopLicenceReviewOverdueService(
  private val licenceRepository: LicenceRepository,
  private val notifyService: NotifyService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun sendComReviewEmail() {
    log.info("Job to runHardStopLicenceReviewOverdueJob started")
    val licencesToReview = licenceRepository.getHardStopLicencesNeedingReview()
    if (licencesToReview.isEmpty()) {
      log.info("Job to runHardStopLicenceReviewOverdueJob has no licences that need reviewing")
      return
    }
    log.info("Sending review emails for ${licencesToReview.size} hard stop licences")
    sendReviewEmailNotification(licencesToReview)
    log.info("Job to runHardStopLicenceReviewOverdueJob finished")
  }

  private fun sendReviewEmailNotification(licences: List<HardStopLicence>) {
    licences.map {
      val com = it.responsibleCom
      if (com != null) {
        notifyService.sendHardStopLicenceReviewOverdueEmail(
          com.email,
          "${com.firstName} ${com.lastName}",
          it.forename!!,
          it.surname!!,
          it.crn,
          it.id.toString(),
        )
      } else {
        log.info("Notification not sent as no COM is attached to licence ${it.id}")
      }
    }
  }
}
