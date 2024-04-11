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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReleaseDateServiceTest {
  private val bankHolidayService = mock<BankHolidayService>()
  private val workingDaysService = WorkingDaysService(bankHolidayService)

  private val service =
    ReleaseDateService(
      clock,
      workingDaysService,
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
    fun `should not warn if allocation is greater than 5 days before release date`() {
      assertFalse(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-21")))
    }

    @Test
    fun `should warn if allocation is 5 days before release date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-20")))
    }

    @Test
    fun `should warn if allocation is 4 days before release date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-16")))
    }

    @Test
    fun `should warn if allocation is 3 days before release date`() {
      assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-15")))
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

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = null,
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse()
    }

    @Test
    fun `licence is not in hard stop period if ARD is after the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = service.getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = cutOff.plusDays(1),
        conditionalReleaseDate = cutOff.plusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse()
    }

    @Test
    fun `licence is in hard stop period if ARD is before the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = service.getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = cutOff.minusDays(1),
        conditionalReleaseDate = cutOff.minusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue()
    }

    @Test
    fun `licence is in hard stop period if ARD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        actualReleaseDate = service.getCutOffDateForLicenceTimeOut(now),
        conditionalReleaseDate = service.getCutOffDateForLicenceTimeOut(now),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue()
    }

    @Test
    fun `licence is still in hard stop period on release day`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        actualReleaseDate = LocalDate.now(now),
        conditionalReleaseDate = LocalDate.now(now),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue()
    }

    @Test
    fun `licence is not in hard stop period after release`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        licenceStartDate = LocalDate.now(now).minusDays(1),
        actualReleaseDate = LocalDate.now(now).minusDays(1),
        conditionalReleaseDate = LocalDate.now(now).minusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse()
    }

    @Test
    fun `licence is in hard stop period if ARD is absent but CRD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = service.getCutOffDateForLicenceTimeOut(now),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue()
    }

    @Test
    fun `returns false if CRD is absent`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        actualReleaseDate = service.getCutOffDateForLicenceTimeOut(now),
        conditionalReleaseDate = null,
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse()
    }
  }

  @Nested
  inner class `Get hard stop date` {
    @Test
    fun `should return null if no conditional release date provided`() {
      val actualReleaseDate = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = actualReleaseDate,
        conditionalReleaseDate = null,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `should return CRD - 2 working days (2024-03-25) if no actual release date provided`() {
      val conditionalReleaseDate = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = conditionalReleaseDate,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 25))
    }

    @Test
    fun `should return ARD - 2 working days (2024-03-25) if ARD is equal to CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 25))
    }

    @Test
    fun `should return ARD - 2 working days (2024-03-22) if ARD is a day before CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(1),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 22))
    }

    @Test
    fun `should return CRD - 2 working days (2024-03-25) if ARD is two or more days before CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(2),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 25))
    }

    @Test
    fun `should return CRD - 2 working days (2024-03-25) if ARD is a day or more after CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.plusDays(1),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 25))
    }
  }

  @Nested
  inner class `Get hard stop warning date` {
    @Test
    fun `should return null if no conditional release date provided`() {
      val actualReleaseDate = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = actualReleaseDate,
        conditionalReleaseDate = null,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `should return CRD - 4 working days (2024-03-21) if no actual release date provided`() {
      val conditionalReleaseDate = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = conditionalReleaseDate,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 21))
    }

    @Test
    fun `should return ARD - 4 working days (2024-03-21) if ARD is equal to CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 21))
    }

    @Test
    fun `should return ARD - 4 working days (2024-03-20) if ARD is a day before CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(1),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 20))
    }

    @Test
    fun `should return CRD - 4 working days (2024-03-21) if ARD is two or more days before CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(2),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 21))
    }

    @Test
    fun `should return CRD - 4 working days (2024-03-21) if ARD is a day or more after CRD`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.plusDays(1),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(LocalDate.of(2024, 3, 21))
    }
  }

  @Nested
  inner class `Licence is eligible for early release` {
    @Test
    fun `should return true if ARD or CRD is Friday(2018-01-05)`() {
      val actualReleaseDate = LocalDate.parse("2018-01-05")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue()
    }

    @Test
    fun `should return true if ARD or CRD is on Saturday(2018-01-06)`() {
      val actualReleaseDate = LocalDate.parse("2018-01-06")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue()
    }

    @Test
    fun `should return true if ARD or CRD is on Sunday(2018-01-07)`() {
      val actualReleaseDate = LocalDate.parse("2018-01-07")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue()
    }

    @Test
    fun `should return true if ARD or CRD is on Bank holiday Friday(2018-03-30)`() {
      val actualReleaseDate = LocalDate.parse("2018-03-30")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue()
    }

    @Test
    fun `should return true if ARD or CRD is on Bank holiday Monday(2018-04-02)`() {
      val actualReleaseDate = LocalDate.parse("2018-04-02")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue()
    }

    @Test
    fun `should return false if ARD or CRD is Thursday (2018-07-05) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-05")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isFalse()
    }

    @Test
    fun `should return false if ARD or CRD is Wednesday (2018-07-04) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-04")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isFalse()
    }

    @Test
    fun `should return false if ARD or CRD is Monday (2018-07-02) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-02")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isFalse()
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

    private fun createClock(timestamp: String) = Clock.fixed(Instant.parse(timestamp), ZoneId.systemDefault())

    val clock: Clock = createClock("2023-11-10T00:00:00Z")
  }
}
