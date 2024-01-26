package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReleaseDateServiceTest {
  private val bankHolidayService = mock<BankHolidayService>()

  private val service =
    ReleaseDateService(
      bankHolidayService,
      clock,
      maxNumberOfWorkingDaysAllowedForEarlyRelease,
      maxNumberOfWorkingDaysToTriggerAllocationWarningEmail,
      maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus,
    )

  @BeforeEach
  fun reset() {
    reset(bankHolidayService)
    whenever(bankHolidayService.getBankHolidaysForEnglandAndWales()).thenReturn(bankHolidays)
  }

  private fun getEarliestDate(actualReleaseDate: LocalDate): LocalDate {
    return service.getEarliestReleaseDate(actualReleaseDate)
  }

  @Nested
  inner class `Earliest release date` {
    @Test
    fun `should return Tuesday(2018-01-02) if ARD or CRD is Friday(2018-01-05) and day they would normally be released is Friday`() {
      val actualReleaseDate = LocalDate.parse("2018-01-05")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
    }

    @Test
    fun `should return Tuesday(2018-01-02) if ARD or CRD is on Saturday(2018-01-06) and day they would normally be released is Friday`() {
      val actualReleaseDate = LocalDate.parse("2018-01-06")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
    }

    @Test
    fun `should return Tuesday(2018-01-02) if ARD or CRD is on Sunday(2018-01-07) and day they would normally be released is Friday`() {
      val actualReleaseDate = LocalDate.parse("2018-01-07")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
    }

    @Test
    fun `should return Tuesday(2018-03-27) if ARD or CRD is on Bank holiday Friday(2018-03-30) and day they would normally be released is Thursday`() {
      val actualReleaseDate = LocalDate.parse("2018-03-30")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return Tuesday(2018-04-27) if ARD or CRD is on Bank holiday Monday(2018-04-02) and day they would normally be released is Friday`() {
      val actualReleaseDate = LocalDate.parse("2018-04-02")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return Tuesday(2018-05-29) if ARD or CRD is on Bank holiday Monday(2018-06-04) Friday(2018-06-01) before also bank holiday and day they would normally be released is Thursday`() {
      val actualReleaseDate = LocalDate.parse("2018-06-04")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-05-29"))
    }

    @Test
    fun `should return Tuesday(2018-11-27) if ARD or CRD is on Monday(2018-12-03) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
      val actualReleaseDate = LocalDate.parse("2018-12-03")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
    }

    @Test
    fun `should return Tuesday(2018-11-27) if ARD or CRD is on Tuesday(2018-12-04) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
      val actualReleaseDate = LocalDate.parse("2018-12-04")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
    }

    @Test
    fun `should return Wednesday(2018-08-01) if ARD or CRD is on Bank holiday Tuesday(2018-08-07) and day they would normally be released is Monday`() {
      val actualReleaseDate = LocalDate.parse("2018-08-07")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-08-01"))
    }

    @Test
    fun `should return Thursday(2018-09-27) if ARD or CRD is on Bank holiday Wednesday(2018-10-03) and day they would normally be released is Tuesday`() {
      val actualReleaseDate = LocalDate.parse("2018-10-03")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-09-27"))
    }

    @Test
    fun `should return (2018-03-27) if ARD is (2018-03-30) third working day before CRD`() {
      val actualReleaseDate = LocalDate.parse("2018-03-30")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return (2018-03-27) if ARD is (2018-04-02) third working day before CRD`() {
      val actualReleaseDate = LocalDate.parse("2018-04-02")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return (2018-04-30) if ARD is (2018-05-07) and (2018-05-02) is bank holiday`() {
      val actualReleaseDate = LocalDate.parse("2018-05-07")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-04-30"))
    }

    @Test
    fun `should return (2018-07-03) if ARD is (2018-07-06) third working day before CRD`() {
      val actualReleaseDate = LocalDate.parse("2018-07-06")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }

    @Test
    fun `should return (2018-07-03) if ARD is (2018-07-07) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-07")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }

    @Test
    fun `should return (2018-07-03) if ARD is (2018-07-08) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-08")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }

    @Test
    fun `should return (2018-07-03) if CRD is (2018-07-08) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-08")

      val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }
  }

  @Nested
  inner class `Late Allocation Warning` {
    @Test
    fun `should not warn if allocation is 6 days before release date`() {
      assertFalse(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-20T00:01", formatter)))
    }

    @Test
    fun `should warn if allocation is 5 days before release date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-17T00:01", formatter)))
    }

    @Test
    fun `should warn if allocation is 4 days before release date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-16")))
    }

    @Test
    fun `should not warn if release date is before allocation date`() {
      assertFalse(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-01")))
    }

    @Test
    fun `should warn if the release date is on allocation date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-10")))
    }

    @Test
    fun `should warn if the release date is after allocation date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-13")))
    }

    @Test
    fun `should return false1 if releaseDate is null`() {
      assertFalse(service.isLateAllocationWarningRequired(null))
    }
  }

  @Nested
  inner class `Licence timeout cutoff date` {
    @Test
    fun `should return cut-off date as 2018-03-14 when job execution date is 2018-03-12`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOffDate = service.getCutOffDateForLicenceTimeOut(now)
      assertTrue(cutOffDate.isEqual(LocalDate.parse("2018-03-14")))
    }

    @Test
    fun `should return cut-off date as 2018-03-20 when job execution date is 2018-03-16 as weekend comes in between`() {
      val now = createClock("2018-03-16T00:00:00Z")
      val cutOffDate = service.getCutOffDateForLicenceTimeOut(now)
      assertTrue(cutOffDate.isEqual(LocalDate.parse("2018-03-20")))
    }

    @Test
    fun `should return cut-off date as 2018-03-28 when job execution date is 2018-03-23 as weekend and one bank holiday comes in between`() {
      val now = createClock("2018-03-23T00:00:00Z")
      val cutOffDate = service.getCutOffDateForLicenceTimeOut(now)
      assertTrue(cutOffDate.isEqual(LocalDate.parse("2018-03-28")))
    }
  }

  @Nested
  inner class `Licence is in hard stop` {
    @Test
    fun `licence is not in hard stop period if there is no release date`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = null,
      )

      assertFalse(service.isInHardStopPeriod(licence, now))
    }

    @Test
    fun `licence is not in hard stop period if ARD is after the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = service.getCutOffDateForLicenceTimeOut(now)

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = cutOff.plusDays(1),
        conditionalReleaseDate = null,
      )

      assertFalse(service.isInHardStopPeriod(licence, now))
    }

    @Test
    fun `licence is in hard stop period if ARD is before the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = service.getCutOffDateForLicenceTimeOut(now)

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = cutOff.minusDays(1),
        conditionalReleaseDate = null,
      )

      assertTrue(service.isInHardStopPeriod(licence, now))
    }

    @Test
    fun `licence is in hard stop period if ARD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = service.getCutOffDateForLicenceTimeOut(now),
        conditionalReleaseDate = null,
      )

      assertTrue(service.isInHardStopPeriod(licence, now))
    }

    @Test
    fun `licence is still in hard stop period on release day`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = LocalDate.now(now),
        conditionalReleaseDate = null,
      )

      assertTrue(service.isInHardStopPeriod(licence, now))
    }

    @Test
    fun `licence is still in hard stop period after release`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = LocalDate.now(now).minusDays(1),
        conditionalReleaseDate = null,
      )

      assertFalse(service.isInHardStopPeriod(licence, now))
    }

    @Test
    fun `licence is in hard stop period if ARD is absent but CRD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = TestData.createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = service.getCutOffDateForLicenceTimeOut(now),
      )

      assertTrue(service.isInHardStopPeriod(licence, now))
    }
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
    const val maxNumberOfWorkingDaysAllowedForEarlyRelease = 3
    const val maxNumberOfWorkingDaysToTriggerAllocationWarningEmail = 6
    const val maxNumberOfWorkingDaysToUpdateLicenceTimeOutStatus = 3
    private fun createClock(timestamp: String) = Clock.fixed(Instant.parse(timestamp), ZoneId.systemDefault())

    val clock: Clock = createClock("2023-11-10T00:00:00Z")
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm]")
  }
}
