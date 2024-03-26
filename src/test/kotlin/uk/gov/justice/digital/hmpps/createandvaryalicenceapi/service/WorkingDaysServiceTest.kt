package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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

  @Nested
  inner class `weekends` {
    @Test
    fun `is date on the weekend`() {
      val today = LocalDate.of(2024, 3, 23)
      val isWeekend = service.isWeekend(today)
      assertThat(isWeekend).isTrue()
    }

    @Test
    fun `is date not on the weekend`() {
      val today = LocalDate.of(2024, 3, 21)
      val isWeekend = service.isWeekend(today)
      assertThat(isWeekend).isFalse()
    }
  }

  @Nested
  inner class `is working day or not` {
    @Test
    fun `is date a non working day`() {
      val today = LocalDate.of(2024, 3, 23)
      val isNonWorkingDay = service.isNonWorkingDay(today)
      assertThat(isNonWorkingDay).isTrue()
    }

    @Test
    fun `is date a working day`() {
      val today = LocalDate.of(2024, 3, 21)
      val isNonWorkingDay = service.isNonWorkingDay(today)
      assertThat(isNonWorkingDay).isFalse()
    }

    @Test
    fun `is bank holiday a non working day`() {
      val today = LocalDate.of(2024, 12, 25)
      val isBankHoliday = service.isNonWorkingDay(today)
      assertThat(isBankHoliday).isTrue()
    }
  }

  @Nested
  inner class `working days after a date` {
    @Test
    fun `get next working day on a weekday`() {
      val today = LocalDate.of(2024, 3, 21)
      val nextWorkingDay = service.workingDaysAfter(today).take(1).first()
      assertThat(LocalDate.of(2024, 3, 22)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `get next working day on a Friday`() {
      val today = LocalDate.of(2024, 3, 22)
      val nextWorkingDay = service.workingDaysAfter(today).take(1).first()
      assertThat(LocalDate.of(2024, 3, 25)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `get next working day on a bank holiday`() {
      val today = LocalDate.of(2024, 5, 3)
      val nextWorkingDay = service.workingDaysAfter(today).take(1).first()
      assertThat(LocalDate.of(2024, 5, 7)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `get next working day on a multi day bank holiday`() {
      val today = LocalDate.of(2024, 3, 28)
      val nextWorkingDay = service.workingDaysAfter(today).take(1).first()
      assertThat(LocalDate.of(2024, 4, 2)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `check sequence is made up of only working days`() {
      val today = LocalDate.of(2024, 3, 21)
      val nextWorkingDays = service.workingDaysAfter(today).take(3)
      nextWorkingDays.forEach {
        assertThat(service.isNonWorkingDay(it)).isFalse()
      }
      assertThat(nextWorkingDays.toList()).isEqualTo(
        listOf(
          LocalDate.of(2024, 3, 22),
          LocalDate.of(2024, 3, 25),
          LocalDate.of(2024, 3, 26),
        ),
      )
    }
  }

  @Nested
  inner class `working days before a date` {
    @Test
    fun `get previous working day on a weekday`() {
      val today = LocalDate.of(2024, 3, 21)
      val previousWorkingDay = service.workingDaysBefore(today).take(1).first()
      assertThat(LocalDate.of(2024, 3, 20)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `get previous working day on a Monday`() {
      val today = LocalDate.of(2024, 3, 25)
      val previousWorkingDay = service.workingDaysBefore(today).take(1).first()
      assertThat(LocalDate.of(2024, 3, 22)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `get previous working day on a bank holiday`() {
      val today = LocalDate.of(2024, 5, 7)
      val previousWorkingDay = service.workingDaysBefore(today).take(1).first()
      assertThat(LocalDate.of(2024, 5, 3)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `get previous working day on a multi day bank holiday`() {
      val today = LocalDate.of(2024, 4, 2)
      val previousWorkingDay = service.workingDaysBefore(today).take(1).first()
      assertThat(LocalDate.of(2024, 3, 28)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `check sequence is made up of only working days`() {
      val today = LocalDate.of(2024, 3, 21)
      val previousWorkingDays = service.workingDaysBefore(today).take(3)
      previousWorkingDays.forEach {
        assertThat(service.isNonWorkingDay(it)).isFalse()
      }
      assertThat(previousWorkingDays.toList()).isEqualTo(
        listOf(
          LocalDate.of(2024, 3, 20),
          LocalDate.of(2024, 3, 19),
          LocalDate.of(2024, 3, 18),
        ),
      )
    }
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
