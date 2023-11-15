package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class ReleaseDateService(
  private val bankHolidayService: BankHolidayService,
  @Value("\${maxNumberOfWorkingDaysAllowedForEarlyRelease}") private val maxNumberOfWorkingDaysAllowedForEarlyRelease: Int,
) {

  /** Friday is also considered as weekend */
  fun isEligibleForEarlyRelease(releaseDate: LocalDate?, includeFriday: Boolean): Boolean {
    val listOfBankHolidays: List<LocalDate> = bankHolidayService.getBankHolidaysForEnglandAndWales()
    val dayOfWeek = releaseDate?.dayOfWeek
    if (getListOfWeekends(includeFriday).any { it === dayOfWeek }) {
      return true
    }
    return listOfBankHolidays.contains(releaseDate)
  }

  private fun getListOfWeekends(includeFriday: Boolean): MutableList<DayOfWeek> {
    val listOfDays = mutableListOf<DayOfWeek>(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    if (includeFriday) {
      listOfDays.add(DayOfWeek.FRIDAY)
    }
    return listOfDays
  }

  fun getEarliestReleaseDate(releaseDate: LocalDate, includeFriday: Boolean, days: Int? = null) =
    generateSequence(releaseDate) { it.minusDays(1) }
      .filterNot { isEligibleForEarlyRelease(it, includeFriday) }
      .take(days ?: maxNumberOfWorkingDaysAllowedForEarlyRelease)
      .last()
}
