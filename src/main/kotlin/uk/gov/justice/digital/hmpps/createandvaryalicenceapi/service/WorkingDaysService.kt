package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class WorkingDaysService(private val bankHolidayService: BankHolidayService) {

  fun addWorkingDays(date: LocalDate, workingDays: Int, isEarlyReleaseWeekend: Boolean = false): LocalDate {
    var adjustedDate = date
    repeat(workingDays) {
      adjustedDate = getNextWorkingDay(adjustedDate, isEarlyReleaseWeekend)
    }
    return adjustedDate
  }

  fun subWorkingDays(date: LocalDate, workingDays: Int, isEarlyReleaseWeekend: Boolean = false): LocalDate {
    var adjustedDate = date
    repeat(workingDays) {
      adjustedDate = getPreviousWorkingDay(adjustedDate, isEarlyReleaseWeekend)
    }
    return adjustedDate
  }

  fun getWorkingDaysRange(
    date: LocalDate,
    workingDaysRequired: Int,
    isPastRangeRequired: Boolean = false,
    isEarlyReleaseWeekend: Boolean = false,
  ): List<LocalDate> {
    var adjustedDate = date
    val workingDays = mutableListOf<LocalDate>()

    repeat(workingDaysRequired) {
      adjustedDate = if (isPastRangeRequired) {
        getPreviousWorkingDay(adjustedDate, isEarlyReleaseWeekend)
      } else {
        getNextWorkingDay(adjustedDate, isEarlyReleaseWeekend)
      }
      workingDays.add(adjustedDate)
    }

    return workingDays.sorted().toList()
  }

  fun isWeekend(date: LocalDate, isEarlyReleaseWeekend: Boolean = false): Boolean {
    val weekend = when (isEarlyReleaseWeekend) {
      true -> listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
      else -> listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
    return date.dayOfWeek in weekend
  }

  fun isNonWorkingDay(date: LocalDate, isEarlyReleaseWeekend: Boolean = false): Boolean {
    return isWeekend(date, isEarlyReleaseWeekend) || getBankHolidays().contains(date)
  }

  fun getNextWorkingDay(date: LocalDate, isEarlyReleaseWeekend: Boolean = false): LocalDate {
    var adjustedDate = date.plusDays(1)
    while (isNonWorkingDay(adjustedDate, isEarlyReleaseWeekend)) {
      adjustedDate = adjustedDate.plusDays(1)
    }
    return adjustedDate
  }

  fun getPreviousWorkingDay(date: LocalDate, isEarlyReleaseWeekend: Boolean = false): LocalDate {
    var adjustedDate = date.minusDays(1)
    while (isNonWorkingDay(adjustedDate, isEarlyReleaseWeekend)) {
      adjustedDate = adjustedDate.minusDays(1)
    }
    return adjustedDate
  }

  private fun getBankHolidays(): List<LocalDate> {
    return bankHolidayService.getBankHolidaysForEnglandAndWales()
  }
}
