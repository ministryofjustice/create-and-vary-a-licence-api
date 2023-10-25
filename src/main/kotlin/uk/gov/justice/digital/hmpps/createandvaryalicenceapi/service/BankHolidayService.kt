package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.GovUkApiClient
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class BankHolidayService(
  private val govUkApiClient: GovUkApiClient,
  @Value("\${workingDays}") private val workingDays: Int,
) {

  @Cacheable("bank-holidays")
  fun getBankHolidaysForEnglandAndWales() = govUkApiClient.getBankHolidaysForEnglandAndWales()

  /** Friday is also considered as weekend */
  fun isBankHolidayOrWeekend(releaseDate: LocalDate?): Boolean {
    val listOfBankHolidays: List<LocalDate> = getBankHolidaysForEnglandAndWales()
    val dayOfWeek = releaseDate?.dayOfWeek
    if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return true
    }
    return listOfBankHolidays.contains(releaseDate)
  }

  fun getEarliestReleaseDate(releaseDate: LocalDate) =
    generateSequence(releaseDate) { it.minusDays(1) }
      .filterNot { isBankHolidayOrWeekend(it) }
      .take(workingDays)
      .last()
}
