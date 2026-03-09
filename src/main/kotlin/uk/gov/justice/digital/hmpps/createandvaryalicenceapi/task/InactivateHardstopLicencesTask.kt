package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.task

import jakarta.persistence.LockTimeoutException
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCaseStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.PotentialHardstopCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.Companion.LICENCE_DEACTIVATION_HARD_STOP_TASK
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

/**
 * Scheduled task to check if hard stop licences that have been moved out of hard stop are
 * still not in the hard stop period a number of hours later, if not, then deactivate them.
 */
@Component
class InactivateHardstopLicencesTask(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val potentialHardstopCaseRepository: PotentialHardstopCaseRepository,
  private val releaseDateService: ReleaseDateService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val TEN_MINUTES_MS = 10 * 60 * 1000L
    val CASE_CREATED_BEFORE = 8.hours
  }

  @Scheduled(fixedDelayString = "1h", initialDelayString = "\${random.long($TEN_MINUTES_MS)}")
  @Transactional
  fun runTask() {
    log.info("Checking for hard stop licences to be inactivated.")

    val dateCreatedBefore = LocalDateTime.now().minusHours(CASE_CREATED_BEFORE.toLong(DurationUnit.HOURS))
    val hardstopCases = try {
      potentialHardstopCaseRepository.findAllByStatusAndDateCreatedBefore(
        PotentialHardstopCaseStatus.PENDING,
        dateCreatedBefore,
      )
    } catch (e: LockTimeoutException) {
      log.warn("Lock timeout when checking for potential hard stop licences to be inactivated: ${e.message}")
      return
    }

    if (hardstopCases.isEmpty()) {
      log.info("No potential hard stop licences found to be inactivated.")
      return
    }

    inactivateLicencesNotInHardstop(hardstopCases)
  }

  private fun inactivateLicencesNotInHardstop(hardstopCases: List<PotentialHardstopCase>) {
    log.info("Inactivate hard stop licences task, checking ${hardstopCases.size} cases")
    hardstopCases.forEach { deactivateLicenceIfNotInHardStop(it) }
  }

  private fun deactivateLicenceIfNotInHardStop(potentialHardStopCase: PotentialHardstopCase) {
    val licence = potentialHardStopCase.licence
    val inHardStop = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind)
    if (!inHardStop) {
      val bookingId = licence.bookingId!!
      log.info("inactivating in flight licences for booking id: $bookingId")
      val licencesToDeactivate = licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        licence.bookingId!!,
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(CRD, HARD_STOP),
      )
      licenceService.inactivateLicences(licencesToDeactivate, LICENCE_DEACTIVATION_HARD_STOP_TASK)
    } else {
      log.info("not inactivating ${licence.id} as it's back in the hard stop period")
    }

    potentialHardStopCase.status = PotentialHardstopCaseStatus.PROCESSED
    potentialHardstopCaseRepository.save(potentialHardStopCase)
  }
}
