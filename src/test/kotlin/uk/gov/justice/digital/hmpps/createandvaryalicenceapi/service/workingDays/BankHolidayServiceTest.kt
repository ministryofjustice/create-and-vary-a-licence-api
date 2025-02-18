package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class BankHolidayServiceTest {
  private val govUkApiClient = mock<GovUkApiClient>()

  private val service = BankHolidayService(govUkApiClient)

  @BeforeEach
  fun reset() {
    reset(govUkApiClient)
    whenever(govUkApiClient.getBankHolidaysForEnglandAndWales()).thenReturn(bankHolidays)
  }

  @Test
  fun `retrieves bank holidays for England and Wales`() {
    whenever(govUkApiClient.getBankHolidaysForEnglandAndWales()).thenReturn(
      listOf(
        LocalDate.parse("2024-09-21"),
      ),
    )

    val result = service.getBankHolidaysForEnglandAndWales()

    verify(govUkApiClient).getBankHolidaysForEnglandAndWales()

    assertThat(result).isNotEmpty
    assertThat(result.size).isEqualTo(1)
    assertThat(result[0]).isEqualTo("2024-09-21")
  }

  private companion object {
    val bankHolidays = listOf(
      LocalDate.parse("2018-01-01"),
      LocalDate.parse("2018-03-26"),
      LocalDate.parse("2018-03-30"),
      LocalDate.parse("2018-04-02"),
      LocalDate.parse("2018-05-02"),
      LocalDate.parse("2018-05-07"),
      LocalDate.parse("2018-06-01"),
      LocalDate.parse("2018-06-04"),
      LocalDate.parse("2018-08-07"),
      LocalDate.parse("2018-10-03"),
      LocalDate.parse("2018-12-03"),
      LocalDate.parse("2018-12-04"),
    )
  }
}
