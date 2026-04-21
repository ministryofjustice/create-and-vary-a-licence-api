package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceReviewRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class LicenceReviewOverdueService(
  private val licenceReviewRepository: LicenceReviewRepository,
  private val notifyService: NotifyService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val REVIEW_LOOKBACK_DAYS = 5L
  }

  @Transactional
  fun sendComReviewEmail() {
    log.info("Job to runLicenceReviewOverdueJob started")
    val (start, end) = calculateDateRange(REVIEW_LOOKBACK_DAYS)
    val licencesToReview = licenceReviewRepository.getLicencesNeedingReview(start, end)
    if (licencesToReview.isEmpty()) {
      log.info("Job to runLicenceReviewOverdueJob has no licences that need reviewing")
      return
    }
    log.info("Sending review emails for ${licencesToReview.size} hard stop licences")
    sendReviewEmailNotification(licencesToReview)
    log.info("Job to runLicenceReviewOverdueJob finished")
  }

  private fun sendReviewEmailNotification(licences: List<Licence>) {
    licences.map {
      val com = it.responsibleCom
      if (com != null) {
        notifyService.sendLicenceReviewOverdueEmail(
          com.email,
          com.fullName,
          it.forename!!,
          it.surname!!,
          it.crn,
          it.id.toString(),
          it.kind == LicenceKind.TIME_SERVED,
        )
      } else {
        log.info("Notification not sent as no COM is attached to licence ${it.id}")
      }
    }
  }

  private fun calculateDateRange(daysAgo: Long): Pair<LocalDateTime, LocalDateTime> {
    val date = LocalDate.now().minusDays(daysAgo)
    return date.atStartOfDay() to date.atTime(LocalTime.MAX)
  }
}
