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
  fun isEligibleForEarlyRelease(releaseDate: LocalDate?): Boolean {
    val listOfBankHolidays: List<LocalDate> = bankHolidayService.getBankHolidaysForEnglandAndWales()
    val dayOfWeek = releaseDate?.dayOfWeek
    if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return true
    }
    return listOfBankHolidays.contains(releaseDate)
  }

  fun getEarliestReleaseDate(releaseDate: LocalDate, days: Int? = null) =
    generateSequence(releaseDate) { it.minusDays(1) }
      .filterNot { isEligibleForEarlyRelease(it) }
      .take(days ?: maxNumberOfWorkingDaysAllowedForEarlyRelease)
      .last()
}
