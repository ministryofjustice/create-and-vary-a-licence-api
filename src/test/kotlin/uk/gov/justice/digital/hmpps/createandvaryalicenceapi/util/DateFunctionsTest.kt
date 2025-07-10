package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateFunctionsTest {
  @Test
  fun `validate null date and actual date`() {
    val date1 = null
    val date2 = LocalDate.of(2021, 10, 22)
    assertThat(date1.hasChanged(date2)).isTrue
  }

  @Test
  fun `validate actual date and null date`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = null
    assertThat(date1.hasChanged(date2)).isTrue
  }

  @Test
  fun `validate two different dates`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = LocalDate.of(2021, 10, 21)
    assertThat(date1.hasChanged(date2)).isTrue
  }

  @Test
  fun `validate two identical dates`() {
    val date1 = LocalDate.of(2021, 10, 22)
    val date2 = LocalDate.of(2021, 10, 22)
    assertThat(date1.hasChanged(date2)).isFalse
  }

  @Test
  fun `validate two null dates`() {
    val date1 = null
    val date2 = null
    assertThat(date1.hasChanged(date2)).isFalse
  }

  @Test
  fun `null is not today or in the future`() {
    assertThat(null.isTodayOrInTheFuture()).isFalse
  }

  @Test
  fun `yesterday is not today or in the future`() {
    assertThat(LocalDate.now().minusDays(1).isTodayOrInTheFuture()).isFalse
  }

  @Test
  fun `today is today or in the future`() {
    assertThat(LocalDate.now().isTodayOrInTheFuture()).isTrue
  }

  @Test
  fun `tomorrow is today or in the future`() {
    assertThat(LocalDate.now().plusDays(1).isTodayOrInTheFuture()).isTrue
  }
}
