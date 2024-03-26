package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class WorkingDaysService(private val bankHolidayService: BankHolidayService) {

  fun workingDaysAfter(
    date: LocalDate,
  ): Sequence<LocalDate> {
    return generateSequence(date) { it.nextWorkingDay() }.drop(1)
  }

  fun workingDaysBefore(
    date: LocalDate,
  ): Sequence<LocalDate> {
    return generateSequence(date) { it.previousWorkingDay() }.drop(1)
  }

  fun isWeekend(date: LocalDate, weekend: List<DayOfWeek> = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)): Boolean {
    return date.dayOfWeek in weekend
  }

  fun isNonWorkingDay(date: LocalDate, weekend: List<DayOfWeek> = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)): Boolean {
    return isWeekend(date, weekend) || getBankHolidays().contains(date)
  }

  fun LocalDate.nextWorkingDay(): LocalDate {
    var adjustedDate = this.plusDays(1)
    while (isNonWorkingDay(adjustedDate)) {
      adjustedDate = adjustedDate.plusDays(1)
    }
    return adjustedDate
  }

  fun LocalDate.previousWorkingDay(): LocalDate {
    var adjustedDate = this.minusDays(1)
    while (isNonWorkingDay(adjustedDate)) {
      adjustedDate = adjustedDate.minusDays(1)
    }
    return adjustedDate
  }

  private fun getBankHolidays(): List<LocalDate> {
    return bankHolidayService.getBankHolidaysForEnglandAndWales()
  }
}
