package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class ReleaseDateService(
  private val bankHolidayService: BankHolidayService,
  private val clock: Clock,
  @Value("\${maxNumberOfWorkingDaysToTriggerAllocationWarningEmail:6}") private val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail: Int,
) {
  fun isLateAllocationWarningRequired(releaseDate: LocalDate?): Boolean {
    if (releaseDate === null) return false
    val dateBeforeXWorkingDays = getEarliestDateBefore(
      maxNumberOfWorkingDaysToTriggerAllocationWarningEmail,
      releaseDate,
      ::excludeBankHolidaysAndWeekends,
    )
    return LocalDate.now(clock).isEqual(dateBeforeXWorkingDays) || LocalDate.now(clock).isAfter(dateBeforeXWorkingDays)
  }

  /** Friday is not considered as weekend */
  private fun excludeBankHolidaysAndWeekends(releaseDate: LocalDate?): Boolean {
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

  fun getEarliestDateBefore(
    days: Int,
    releaseDate: LocalDate,
    isEligibleForEarlyRelease: (input: LocalDate) -> Boolean,
  ): LocalDate =
    generateSequence(releaseDate) { it.minusDays(1) }
      .filterNot { isEligibleForEarlyRelease(it) }
      .take(days)
      .last()
}
