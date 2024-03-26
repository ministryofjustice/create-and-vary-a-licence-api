package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.nullableDatesDiffer
import java.time.LocalDate

class DateFunctionsTest {
  @Test
  fun `validate null date and actual date`() {
    val date1 = null
    val date2 = LocalDate.of(2021, 10, 22)
    assertThat(nullableDatesDiffer(date1, date2)).isTrue
  }

  @Test
  fun `validate actual date and null date`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = null
    assertThat(nullableDatesDiffer(date1, date2)).isTrue
  }

  @Test
  fun `validate two different dates`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = LocalDate.of(2021, 10, 21)
    assertThat(nullableDatesDiffer(date1, date2)).isTrue
  }

  @Test
  fun `validate two identical dates`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = LocalDate.of(2021, 10, 22)
    assertThat(nullableDatesDiffer(date1, date2)).isFalse
  }

  @Test
  fun `validate two null dates`() {
    val date1 = null
    val date2 = null
    assertThat(nullableDatesDiffer(date1, date2)).isFalse
  }
}
