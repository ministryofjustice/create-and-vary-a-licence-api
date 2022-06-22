package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateFunctionsTest {
  @Test
  fun `validate null date and actual date`() {
    val date1 = null
    val date2 = LocalDate.of(2021, 10, 22)
    assertTrue(nullableDatesDiffer(date1, date2))
  }

  @Test
  fun `validate actual date and null date`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = null
    assertTrue(nullableDatesDiffer(date1, date2))
  }

  @Test
  fun `validate two different dates`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = LocalDate.of(2021, 10, 21)
    assertTrue(nullableDatesDiffer(date1, date2))
  }

  @Test
  fun `validate two identical dates`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = LocalDate.of(2021, 10, 22)
    assertFalse(nullableDatesDiffer(date1, date2))
  }

  @Test
  fun `validate two null dates`() {
    val date1 = null
    val date2 = null
    assertFalse(nullableDatesDiffer(date1, date2))
  }
}
