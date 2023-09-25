package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.gov.GovUkApiClient
import java.time.LocalDate

class BankHolidayServiceTest {
  private val govUkApiClient = mock<GovUkApiClient>()

  private val service = BankHolidayService(govUkApiClient)

  @BeforeEach
  fun reset() {
    reset(govUkApiClient)
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
}
