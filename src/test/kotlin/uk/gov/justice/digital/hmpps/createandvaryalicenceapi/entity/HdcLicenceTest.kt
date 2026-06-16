package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import java.time.LocalTime

class HdcLicenceTest {

  @Nested
  inner class IsCurfewSameTimeEachDayTest {
    val time1 = CurfewTimes(fromTime = LocalTime.of(20, 0), untilTime = LocalTime.of(6, 0))
    val time2 = CurfewTimes(fromTime = LocalTime.of(19, 0), untilTime = LocalTime.of(5, 0))

    @Test
    fun `same time each day with no times`() {
      val hdcLicence = createHdcLicence()
      hdcLicence.weeklyCurfewTimes = mutableListOf()
      assertThat(hdcLicence.isCurfewSameTimeEachDay()).isTrue()
    }

    @Test
    fun `same time each day with one time`() {
      val hdcLicence = createHdcLicence()
      hdcLicence.weeklyCurfewTimes + time1
      assertThat(hdcLicence.isCurfewSameTimeEachDay()).isTrue()
    }

    @Test
    fun `same time each day with multiple times all same`() {
      val hdcLicence = createHdcLicence()
      hdcLicence.weeklyCurfewTimes + time1 + time1 + time1
      assertThat(hdcLicence.isCurfewSameTimeEachDay()).isTrue()
    }

    @Test
    fun `same time each day with multiple times one different`() {
      val hdcLicence = createHdcLicence()
      hdcLicence.weeklyCurfewTimes + time1 + time2 + time1
      assertThat(hdcLicence.isCurfewSameTimeEachDay()).isTrue()
    }
  }
}
