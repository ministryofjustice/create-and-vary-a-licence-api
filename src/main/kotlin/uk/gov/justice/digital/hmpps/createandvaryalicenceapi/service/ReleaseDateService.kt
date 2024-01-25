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
  private val bankHolidayService: BankHolidayService,
  private val clock: Clock,
  @Value("\${maxNumberOfWorkingDaysAllowedForEarlyRelease:3}") private val maxNumberOfWorkingDaysAllowedForEarlyRelease: Int,
  @Value("\${maxNumberOfWorkingDaysToTriggerAllocationWarningEmail:6}") private val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail: Int,
  @Value("\${maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus:3}") private val maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus: Int,
) {

  fun getCutOffDateForLicenceTimeOut(now: Clock? = null): LocalDate {
    return generateSequence(LocalDate.now(now ?: clock)) { it.plusDays(1) }
      .filterNot { isBankHolidayOrWeekend(it) }
      .take(maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus)
      .last()
  }

  fun isInHardStopPeriod(licence: Licence, overrideClock: Clock? = null): Boolean {
    val now = overrideClock ?: clock
    val endOfHardStopPeriod = getCutOffDateForLicenceTimeOut(now)
    val releaseDate = licence.actualReleaseDate ?: licence.conditionalReleaseDate

    if (releaseDate == null) {
      log.warn("Licence with id: ${licence.id} has no CRD or ARD")
      return false
    }
    return if (releaseDate < LocalDate.now(now)) {
      false
    } else {
      releaseDate <= endOfHardStopPeriod
    }
  }

  fun getEarliestReleaseDate(releaseDate: LocalDate): LocalDate {
    return getEarliestDateBefore(maxNumberOfWorkingDaysAllowedForEarlyRelease, releaseDate, ::isEligibleForEarlyRelease)
  }

  fun isLateAllocationWarningRequired(releaseDate: LocalDate?): Boolean {
    if (releaseDate === null || releaseDate.isBefore(LocalDate.now(clock))) return false
    val warningThreshold = getEarliestDateBefore(
      maxNumberOfWorkingDaysToTriggerAllocationWarningEmail,
      releaseDate,
      ::isBankHolidayOrWeekend,
    )
    return LocalDate.now(clock) >= warningThreshold
  }

  /** Friday is not considered as weekend */
  fun isBankHolidayOrWeekend(releaseDate: LocalDate?): Boolean {
    val dayOfWeek = releaseDate?.dayOfWeek
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return true
    }
    val listOfBankHolidays: List<LocalDate> = bankHolidayService.getBankHolidaysForEnglandAndWales()
    return listOfBankHolidays.contains(releaseDate)
  }

  /** Friday is also considered as weekend */
  fun isEligibleForEarlyRelease(releaseDate: LocalDate): Boolean {
    val listOfBankHolidays: List<LocalDate> = bankHolidayService.getBankHolidaysForEnglandAndWales()
    val dayOfWeek = releaseDate.dayOfWeek
    if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return true
    }
    return listOfBankHolidays.contains(releaseDate)
  }

  private fun getEarliestDateBefore(
    days: Int,
    releaseDate: LocalDate,
    isEligibleForEarlyRelease: (input: LocalDate) -> Boolean,
  ): LocalDate =
    generateSequence(releaseDate) { it.minusDays(1) }
      .filterNot { isEligibleForEarlyRelease(it) }
      .take(days)
      .last()

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
