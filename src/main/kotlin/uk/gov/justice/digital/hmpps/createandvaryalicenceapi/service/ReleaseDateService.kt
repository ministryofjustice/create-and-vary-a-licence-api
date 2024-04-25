package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
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
    return maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus.workingDaysAfter(LocalDate.now(now ?: clock))
  }

  fun isInHardStopPeriod(sentenceDateHolder: SentenceDateHolder, overrideClock: Clock? = null): Boolean {
    val now = overrideClock ?: clock
    val hardStopDate = getHardStopDate(sentenceDateHolder)
    val today = LocalDate.now(now)

    if (hardStopDate == null || sentenceDateHolder.licenceStartDate == null) {
      return false
    }

    return today >= hardStopDate && today <= sentenceDateHolder.licenceStartDate
  }

  fun isDueForEarlyRelease(sentenceDateHolder: SentenceDateHolder): Boolean {
    val actualReleaseDate = sentenceDateHolder.actualReleaseDate
    val conditionalReleaseDate = sentenceDateHolder.conditionalReleaseDate

    if (actualReleaseDate == null || conditionalReleaseDate == null) {
      return false
    }
    return actualReleaseDate < 1.workingDaysBefore(conditionalReleaseDate)
  }

  fun isDueToBeReleasedInTheNextTwoWorkingDays(sentenceDateHolder: SentenceDateHolder): Boolean {
    val actualReleaseDate = sentenceDateHolder.actualReleaseDate
    val conditionalReleaseDate = sentenceDateHolder.conditionalReleaseDate
    val earliestReleaseDate = listOfNotNull(actualReleaseDate, conditionalReleaseDate).minOrNull() ?: return false
    return LocalDate.now(clock) >= 2.workingDaysBefore(earliestReleaseDate) && LocalDate.now(clock) <= earliestReleaseDate
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

  fun getHardStopDate(sentenceDateHolder: SentenceDateHolder): LocalDate? {
    val actualReleaseDate = sentenceDateHolder.actualReleaseDate
    val conditionalReleaseDate = sentenceDateHolder.conditionalReleaseDate ?: return null

    val date = chooseDateForHardstop(actualReleaseDate, conditionalReleaseDate)

    return 2.workingDaysBefore(date)
  }

  fun getHardStopWarningDate(sentenceDateHolder: SentenceDateHolder): LocalDate? {
    val hardStopDate = getHardStopDate(sentenceDateHolder) ?: return null
    return 2.workingDaysBefore(hardStopDate)
  }

  private fun chooseDateForHardstop(actualReleaseDate: LocalDate?, conditionalReleaseDate: LocalDate): LocalDate {
    if (actualReleaseDate == null) {
      return conditionalReleaseDate
    }

    val isNotAnEarlyRelease = actualReleaseDate >= 1.workingDaysBefore(conditionalReleaseDate)
    val isArdTheReleaseDate = actualReleaseDate <= conditionalReleaseDate

    return if (isNotAnEarlyRelease && isArdTheReleaseDate) {
      actualReleaseDate
    } else {
      conditionalReleaseDate
    }
  }

  private fun Int.workingDaysBefore(date: LocalDate) = workingDaysService.workingDaysBefore(date).take(this).last()

  private fun Int.workingDaysAfter(date: LocalDate) = workingDaysService.workingDaysAfter(date).take(this).last()

  private fun getEarliestDateBefore(
    days: Int,
    releaseDate: LocalDate,
  ): LocalDate = workingDaysService.workingDaysBefore(releaseDate)
    .filterNot { it.dayOfWeek == DayOfWeek.FRIDAY }
    .take(days)
    .last()
}
