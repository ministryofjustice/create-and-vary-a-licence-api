package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.task

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PotentialHardstopCaseStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.PotentialHardstopCaseRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.Companion.LICENCE_DEACTIVATION_HARD_STOP_TASK
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import java.time.LocalDateTime

/**
 * Scheduled task to ensure cache is refreshed at regular intervals.
 */
@Component
class InactivateHardstopLicencesTask(
  private val licenceService: LicenceService,
  private val potentialHardstopCaseRepository: PotentialHardstopCaseRepository,
  private val releaseDateService: ReleaseDateService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val TEN_MINUTES_MS = 10 * 60 * 1000L
    const val CASE_CREATED_BEFORE_HOURS = 8L
  }

  @Scheduled(fixedDelayString = "1h", initialDelayString = "\${random.long($TEN_MINUTES_MS)}")
  @Transactional
  fun runTask() {
    log.info("Checking for hard stop licences to be inactivated.")

    val dateCreatedBefore = LocalDateTime.now().minusHours(CASE_CREATED_BEFORE_HOURS)
    val hardstopCases =
      potentialHardstopCaseRepository.findAllByStatusAndDateCreatedBefore(
        PotentialHardstopCaseStatus.PENDING,
        dateCreatedBefore,
      )

    if (hardstopCases.isEmpty()) {
      log.info("No potential hard stop licences found to be inactivated.")
      return
    }

    inactivateLicencesNotInHardstop(hardstopCases)
  }

  private fun inactivateLicencesNotInHardstop(hardstopCases: List<PotentialHardstopCase>) {
    hardstopCases.forEach { deactivateLicenceIfNotInHardStop(it) }
  }

  private fun deactivateLicenceIfNotInHardStop(potentialHardStopCase: PotentialHardstopCase) {
    val licence = potentialHardStopCase.licence
    val inHardStop = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind)
    if (!inHardStop) {
      licenceService.inactivateLicences(listOf(licence), LICENCE_DEACTIVATION_HARD_STOP_TASK)
    }

    potentialHardStopCase.status = PotentialHardstopCaseStatus.PROCESSED
    potentialHardstopCaseRepository.save(potentialHardStopCase)
  }
}
