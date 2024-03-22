package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDate

class WorkingDaysServiceTest {
  private val bankHolidayService = mock<BankHolidayService>()
  private val service = WorkingDaysService(bankHolidayService)

  @BeforeEach
  fun reset() {
    reset(bankHolidayService)
    whenever(bankHolidayService.getBankHolidaysForEnglandAndWales()).thenReturn(someBankHolidays)
  }

  @Test
  fun `is date on the weekend`() {
    val today = LocalDate.of(2024, 3, 23)
    val isWeekend = service.isWeekend(today)
    assertTrue(isWeekend)
  }

  @Test
  fun `is date not on the weekend`() {
    val today = LocalDate.of(2024, 3, 21)
    val isWeekend = service.isWeekend(today)
    assertFalse(isWeekend)
  }

  @Test
  fun `is date a non working day`() {
    val today = LocalDate.of(2024, 3, 23)
    val isNonWorkingDay = service.isNonWorkingDay(someBankHolidays, today)
    assertTrue(isNonWorkingDay)
  }

  @Test
  fun `is date a working day`() {
    val today = LocalDate.of(2024, 3, 21)
    val isWorkingDay = service.isNonWorkingDay(someBankHolidays, today)
    assertFalse(isWorkingDay)
  }

  @Test
  fun `get next working day on a weekday`() {
    val today = LocalDate.of(2024, 3, 21)
    val nextWorkingDay = service.getNextWorkingDay(someBankHolidays, today)
    assertEquals(nextWorkingDay, LocalDate.of(2024, 3, 22))
  }

  @Test
  fun `get next working day on a Friday`() {
    val today = LocalDate.of(2024, 3, 22)
    val nextWorkingDay = service.getNextWorkingDay(someBankHolidays, today)
    assertEquals(nextWorkingDay, LocalDate.of(2024, 3, 25))
  }

  @Test
  fun `get next working day on a bank holiday`() {
    val today = LocalDate.of(2024, 5, 3)
    val nextWorkingDay = service.getNextWorkingDay(someBankHolidays, today)
    assertEquals(nextWorkingDay, LocalDate.of(2024, 5, 7))
  }

  @Test
  fun `get next working day on a multi day bank holiday`() {
    val today = LocalDate.of(2024, 3, 28)
    val nextWorkingDay = service.getNextWorkingDay(someBankHolidays, today)
    assertEquals(nextWorkingDay, LocalDate.of(2024, 4, 2))
  }

  @Test
  fun `is bank holiday a non working day`() {
    val today = LocalDate.of(2024, 12, 25)
    val isBankHoliday = service.isNonWorkingDay(someBankHolidays, today)
    assertTrue(isBankHoliday)
  }

  @Test
  fun `add working days`() {
    val today = LocalDate.of(2024, 3, 20)
    val date = service.addWorkingDays(today, 2)
    assertEquals(date, LocalDate.of(2024, 3, 22))
  }

  @Test
  fun `add working days on Friday`() {
    val today = LocalDate.of(2024, 3, 22)
    val date = service.addWorkingDays(today, 2)
    assertEquals(date, LocalDate.of(2024, 3, 26))
  }

  @Test
  fun `add working days on Easter bank holiday`() {
    val today = LocalDate.of(2024, 3, 28)
    val date = service.addWorkingDays(today, 2)
    assertEquals(date, LocalDate.of(2024, 4, 3))
  }

  @Test
  fun `add working days on multiple Christmas bank holidays`() {
    val today = LocalDate.of(2024, 12, 20)
    val date = service.addWorkingDays(today, 10)
    assertEquals(date, LocalDate.of(2025, 1, 8))
  }

  private companion object {
    val someBankHolidays = listOf(
      LocalDate.parse("2024-03-29"),
      LocalDate.parse("2024-04-01"),
      LocalDate.parse("2024-05-06"),
      LocalDate.parse("2024-08-26"),
      LocalDate.parse("2024-12-25"),
      LocalDate.parse("2024-12-26"),
      LocalDate.parse("2025-01-01"),
    )
  }
}
