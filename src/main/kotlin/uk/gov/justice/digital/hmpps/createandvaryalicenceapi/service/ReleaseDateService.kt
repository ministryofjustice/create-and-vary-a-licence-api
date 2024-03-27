package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class ReleaseDateService(
  private val clock: Clock,
  private val workingDaysService: WorkingDaysService,
  @Value("\${maxNumberOfWorkingDaysAllowedForEarlyRelease:3}") private val maxNumberOfWorkingDaysAllowedForEarlyRelease: Int = 3,
  @Value("\${maxNumberOfWorkingDaysToTriggerAllocationWarningEmail:5}") private val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail: Int = 5,
  @Value("\${maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus:2}") private val maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus: Int = 2,
) {

  fun getCutOffDateForLicenceTimeOut(now: Clock? = null): LocalDate {
    return workingDaysService.workingDaysAfter(LocalDate.now(now ?: clock))
      .take(maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus)
      .last()
  }

  fun isInHardStopPeriod(licence: Licence, overrideClock: Clock? = null): Boolean {
    val now = overrideClock ?: clock
    val hardStopDate = getHardStopDate(licence)
    val today = LocalDate.now(now)

    if (hardStopDate == null) {
      log.warn("Licence with id: ${licence.id} has no CRD or ARD")
      return false
    }

    return today >= hardStopDate && today <= licence.licenceStartDate
  }

  fun getEarliestReleaseDate(releaseDate: LocalDate): LocalDate {
    return getEarliestDateBefore(maxNumberOfWorkingDaysAllowedForEarlyRelease, releaseDate)
  }

  fun isLateAllocationWarningRequired(releaseDate: LocalDate?): Boolean {
    if (releaseDate === null || releaseDate.isBefore(LocalDate.now(clock))) return false
    val warningThreshold = getEarliestDateBefore(
      maxNumberOfWorkingDaysToTriggerAllocationWarningEmail,
      releaseDate,
    )
    return LocalDate.now(clock) >= warningThreshold
  }

  /** Friday is also considered as weekend */
  fun isEligibleForEarlyRelease(releaseDate: LocalDate): Boolean {
    if (releaseDate.dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
      return true
    }
    return workingDaysService.isNonWorkingDay(releaseDate)
  }

  fun getHardStopDate(licence: Licence): LocalDate? {
    val actualReleaseDate = licence.actualReleaseDate
    val conditionalReleaseDate = licence.conditionalReleaseDate

    if (conditionalReleaseDate == null && actualReleaseDate == null) {
      return null
    }

    if (conditionalReleaseDate != null) {
      val hardStopDateUsingCrd = workingDaysService.workingDaysBefore(conditionalReleaseDate).take(maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus).last()
      if (actualReleaseDate != null) {
        val hardStopDateUsingArd = workingDaysService.workingDaysBefore(actualReleaseDate).take(maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus).last()
        val isNotAnEarlyRelease = actualReleaseDate >= workingDaysService.workingDaysBefore(conditionalReleaseDate).take(1).last()
        val isArdTheReleaseDate = actualReleaseDate <= conditionalReleaseDate
        return if (isNotAnEarlyRelease && isArdTheReleaseDate) {
          hardStopDateUsingArd
        } else {
          hardStopDateUsingCrd
        }
      } else {
        return hardStopDateUsingCrd
      }
    } else {
      return null
    }
  }

  private fun getEarliestDateBefore(
    days: Int,
    releaseDate: LocalDate,
  ): LocalDate = workingDaysService.workingDaysBefore(releaseDate)
    .filterNot { it.dayOfWeek == DayOfWeek.FRIDAY }
    .take(days)
    .last()

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
