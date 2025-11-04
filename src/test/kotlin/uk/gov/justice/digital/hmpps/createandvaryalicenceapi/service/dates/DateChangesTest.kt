package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateChangesTest {

  @Test
  fun `date change description should contain the date type and the new date`() {
    val dateType = LicenceDateType.PRRD
    val newDate = LocalDate.of(2023, 1, 1)
    val dateChange = DateChange(LicenceDateType.PRRD, newDate, LocalDate.of(2025, 6, 15))

    assertThat(dateChange.toDescription()).isEqualTo("${dateType.description} has changed to ${newDate.format(dateFormat)}")
  }

  @Test
  fun `date change description should explain that a date has been removed`() {
    val dateType = LicenceDateType.PRRD
    val newDate = null
    val dateChange = DateChange(LicenceDateType.PRRD, newDate, LocalDate.of(2025, 6, 15))

    assertThat(dateChange.toDescription()).isEqualTo("${dateType.description} has been removed")
  }
}
