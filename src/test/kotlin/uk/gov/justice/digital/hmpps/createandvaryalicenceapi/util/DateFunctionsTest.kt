package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateFunctionsTest {

  @Nested
  inner class HasChanged {
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
  }

  @Nested
  inner class IsTodayOrInTheFuture {
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

  @Nested
  inner class IsOnOrBefore {
    @Test
    fun `returns false if the comparison date is null`() {
      assertThat(LocalDate.now().isOnOrBefore(null)).isFalse
    }

    @Test
    fun `returns true if the calling date is before the comparison date`() {
      assertThat(LocalDate.now().minusDays(1).isOnOrBefore(LocalDate.now())).isTrue()
    }

    @Test
    fun `returns true if the calling date is equal to the comparison date`() {
      assertThat(LocalDate.now().isOnOrBefore(LocalDate.now())).isTrue()
    }

    @Test
    fun `returns false if the calling date is after the comparison date`() {
      assertThat(LocalDate.now().plusDays(1).isOnOrBefore(LocalDate.now())).isFalse()
    }
  }
}
