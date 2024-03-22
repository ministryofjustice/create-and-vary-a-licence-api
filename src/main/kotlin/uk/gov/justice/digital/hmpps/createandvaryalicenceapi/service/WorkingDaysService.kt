package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class WorkingDaysService(private val bankHolidayService: BankHolidayService) {

  fun addWorkingDays(date: LocalDate, workingDays: Int): LocalDate {
    val bankHolidays = bankHolidayService.getBankHolidaysForEnglandAndWales()
    var adjustedDate = date
    for (i in 0 until workingDays) {
      adjustedDate = getNextWorkingDay(bankHolidays, adjustedDate)
    }
    return adjustedDate
  }

  fun isWeekend(date: LocalDate): Boolean {
    val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    return date.dayOfWeek in weekend
  }

  fun isNonWorkingDay(bankHolidays: List<LocalDate>, date: LocalDate): Boolean {
    return isWeekend(date) || bankHolidays.contains(date)
  }

  fun getNextWorkingDay(bankHolidays: List<LocalDate>, date: LocalDate): LocalDate {
    var adjustedDate = date.plusDays(1)
    while (isNonWorkingDay(bankHolidays, adjustedDate)) {
      adjustedDate = adjustedDate.plusDays(1)
    }
    return adjustedDate
  }
}
