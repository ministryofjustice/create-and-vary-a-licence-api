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

    @Test
    fun `is date on the early release weekend`() {
      val today = LocalDate.of(2024, 3, 22)
      val isERWeekend = service.isWeekend(today, true)
      assertThat(isERWeekend).isTrue()
    }

    @Test
    fun `is date not on the early release weekend`() {
      val today = LocalDate.of(2024, 3, 21)
      val isERWeekend = service.isWeekend(today, true)
      assertThat(isERWeekend).isFalse()
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
  inner class `next working day` {
    @Test
    fun `get next working day on a weekday`() {
      val today = LocalDate.of(2024, 3, 21)
      val nextWorkingDay = service.getNextWorkingDay(today)
      assertThat(LocalDate.of(2024, 3, 22)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `get next working day on a Friday`() {
      val today = LocalDate.of(2024, 3, 22)
      val nextWorkingDay = service.getNextWorkingDay(today)
      assertThat(LocalDate.of(2024, 3, 25)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `get next working day on a bank holiday`() {
      val today = LocalDate.of(2024, 5, 3)
      val nextWorkingDay = service.getNextWorkingDay(today)
      assertThat(LocalDate.of(2024, 5, 7)).isEqualTo(nextWorkingDay)
    }

    @Test
    fun `get next working day on a multi day bank holiday`() {
      val today = LocalDate.of(2024, 3, 28)
      val nextWorkingDay = service.getNextWorkingDay(today)
      assertThat(LocalDate.of(2024, 4, 2)).isEqualTo(nextWorkingDay)
    }
  }

  @Nested
  inner class `previous working day` {
    @Test
    fun `get previous working day on a weekday`() {
      val today = LocalDate.of(2024, 3, 21)
      val previousWorkingDay = service.getPreviousWorkingDay(today)
      assertThat(LocalDate.of(2024, 3, 20)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `get previous working day on a Monday`() {
      val today = LocalDate.of(2024, 3, 25)
      val previousWorkingDay = service.getPreviousWorkingDay(today)
      assertThat(LocalDate.of(2024, 3, 22)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `get previous working day on a bank holiday`() {
      val today = LocalDate.of(2024, 5, 7)
      val previousWorkingDay = service.getPreviousWorkingDay(today)
      assertThat(LocalDate.of(2024, 5, 3)).isEqualTo(previousWorkingDay)
    }

    @Test
    fun `get previous working day on a multi day bank holiday`() {
      val today = LocalDate.of(2024, 4, 2)
      val previousWorkingDay = service.getPreviousWorkingDay(today)
      assertThat(LocalDate.of(2024, 3, 28)).isEqualTo(previousWorkingDay)
    }
  }

  @Nested
  inner class `add working days` {
    @Test
    fun `add working days`() {
      val today = LocalDate.of(2024, 3, 20)
      val date = service.addWorkingDays(today, 2)
      assertThat(LocalDate.of(2024, 3, 22)).isEqualTo(date)
    }

    @Test
    fun `add working days on Friday`() {
      val today = LocalDate.of(2024, 3, 22)
      val date = service.addWorkingDays(today, 2)
      assertThat(LocalDate.of(2024, 3, 26)).isEqualTo(date)
    }

    @Test
    fun `add working days over Easter bank holiday`() {
      val today = LocalDate.of(2024, 3, 28)
      val date = service.addWorkingDays(today, 2)
      assertThat(LocalDate.of(2024, 4, 3)).isEqualTo(date)
    }

    @Test
    fun `add working days over multiple Christmas bank holidays`() {
      val today = LocalDate.of(2024, 12, 20)
      val date = service.addWorkingDays(today, 10)
      assertThat(LocalDate.of(2025, 1, 8)).isEqualTo(date)
    }
  }

  @Nested
  inner class `subtract working days` {
    @Test
    fun `subtract working days`() {
      val today = LocalDate.of(2024, 3, 20)
      val date = service.subWorkingDays(today, 2)
      assertThat(LocalDate.of(2024, 3, 18)).isEqualTo(date)
    }

    @Test
    fun `subtract working days on Monday`() {
      val today = LocalDate.of(2024, 3, 18)
      val date = service.subWorkingDays(today, 2)
      assertThat(LocalDate.of(2024, 3, 14)).isEqualTo(date)
    }

    @Test
    fun `subtract working days over Easter bank holiday`() {
      val today = LocalDate.of(2024, 4, 3)
      val date = service.subWorkingDays(today, 2)
      assertThat(LocalDate.of(2024, 3, 28)).isEqualTo(date)
    }

    @Test
    fun `subtract working days over multiple Christmas bank holidays`() {
      val today = LocalDate.of(2025, 1, 8)
      val date = service.subWorkingDays(today, 10)
      assertThat(LocalDate.of(2024, 12, 20)).isEqualTo(date)
    }
  }

  @Nested
  inner class `working date ranges` {
    @Test
    fun `get working days range in the future`() {
      val today = LocalDate.of(2024, 3, 20)
      val dates = service.getWorkingDaysRange(today, 2)
      assertThat(listOf(LocalDate.of(2024, 3, 21), LocalDate.of(2024, 3, 22))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the past`() {
      val today = LocalDate.of(2024, 3, 20)
      val dates = service.getWorkingDaysRange(today, 2, true)
      assertThat(listOf(LocalDate.of(2024, 3, 18), LocalDate.of(2024, 3, 19))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the future on Friday`() {
      val today = LocalDate.of(2024, 3, 22)
      val dates = service.getWorkingDaysRange(today, 2)
      assertThat(listOf(LocalDate.of(2024, 3, 25), LocalDate.of(2024, 3, 26))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the past on Monday`() {
      val today = LocalDate.of(2024, 3, 25)
      val dates = service.getWorkingDaysRange(today, 2, true)
      assertThat(listOf(LocalDate.of(2024, 3, 21), LocalDate.of(2024, 3, 22))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the future over a bank holiday`() {
      val today = LocalDate.of(2024, 5, 3)
      val dates = service.getWorkingDaysRange(today, 2)
      assertThat(listOf(LocalDate.of(2024, 5, 7), LocalDate.of(2024, 5, 8))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the past over a bank holiday`() {
      val today = LocalDate.of(2024, 5, 7)
      val dates = service.getWorkingDaysRange(today, 2, true)
      assertThat(listOf(LocalDate.of(2024, 5, 2), LocalDate.of(2024, 5, 3))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the future over a multi bank holiday`() {
      val today = LocalDate.of(2024, 3, 28)
      val dates = service.getWorkingDaysRange(today, 2)
      assertThat(listOf(LocalDate.of(2024, 4, 2), LocalDate.of(2024, 4, 3))).isEqualTo(dates)
    }

    @Test
    fun `get working days range in the past over a multi bank holiday`() {
      val today = LocalDate.of(2024, 4, 2)
      val dates = service.getWorkingDaysRange(today, 2, true)
      assertThat(listOf(LocalDate.of(2024, 3, 27), LocalDate.of(2024, 3, 28))).isEqualTo(dates)
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
