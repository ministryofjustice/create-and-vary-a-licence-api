package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReleaseDateServiceTest {
  private val iS91DeterminationService = mock<IS91DeterminationService>()
  private val bankHolidayService = mock<BankHolidayService>()
  private val workingDaysService = WorkingDaysService(bankHolidayService)

  private val service =
    ReleaseDateService(
      clock,
      workingDaysService,
      iS91DeterminationService,
    )

  @BeforeEach
  fun reset() {
    reset(bankHolidayService)
    whenever(bankHolidayService.getBankHolidaysForEnglandAndWales()).thenReturn(bankHolidays)
  }

  private fun getEarliestDate(actualReleaseDate: LocalDate): LocalDate? {
    return service.getEarliestReleaseDate(
      object : SentenceDateHolder {
        override val licenceStartDate: LocalDate? = null
        override val conditionalReleaseDate: LocalDate? = null
        override val actualReleaseDate: LocalDate? = actualReleaseDate
      },
    )
  }

  fun getCutOffDateForLicenceTimeOut(now: Clock? = null): LocalDate {
    return workingDaysService.workingDaysAfter(LocalDate.now(now)).take(2).last()
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
      val cutOffDate = getCutOffDateForLicenceTimeOut(now)
      assertTrue(cutOffDate.isEqual(LocalDate.parse("2018-03-14")))
    }

    @Test
    fun `should return cut-off date as 2018-03-20 when job execution date is 2018-03-16 as weekend comes in between`() {
      val now = createClock("2018-03-16T00:00:00Z")
      val cutOffDate = getCutOffDateForLicenceTimeOut(now)
      assertTrue(cutOffDate.isEqual(LocalDate.parse("2018-03-20")))
    }

    @Test
    fun `should return cut-off date as 2018-03-28 when job execution date is 2018-03-23 as weekend and one bank holiday comes in between`() {
      val now = createClock("2018-03-23T00:00:00Z")
      val cutOffDate = getCutOffDateForLicenceTimeOut(now)
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

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse
    }

    @Test
    fun `licence is not in hard stop period if ARD is after the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = cutOff.plusDays(1),
        conditionalReleaseDate = cutOff.plusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse
    }

    @Test
    fun `licence is in hard stop period if ARD is before the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = cutOff.minusDays(1),
        conditionalReleaseDate = cutOff.minusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue
    }

    @Test
    fun `licence is in hard stop period if ARD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = cutOff,
        conditionalReleaseDate = cutOff,
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue
    }

    @Test
    fun `licence is still in hard stop period on release day`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        actualReleaseDate = LocalDate.now(now),
        conditionalReleaseDate = LocalDate.now(now),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue
    }

    @Test
    fun `licence is not in hard stop period after release`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        licenceStartDate = LocalDate.now(now).minusDays(1),
        actualReleaseDate = LocalDate.now(now).minusDays(1),
        conditionalReleaseDate = LocalDate.now(now).minusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse
    }

    @Test
    fun `licence is in hard stop period if ARD is absent but CRD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = cutOff,
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isTrue
    }

    @Test
    fun `returns false if CRD is absent`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = cutOff,
        licenceStartDate = null,
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse
    }

    @Test
    fun `returns false if licence start date is absent`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        actualReleaseDate = cutOff,
        conditionalReleaseDate = null,
      )

      assertThat(service.isInHardStopPeriod(licence, now)).isFalse
    }
  }

  @Nested
  inner class `Licence is due to be released in the next two days` {
    val thisClock = createClock("2024-04-22T00:00:00Z")
    val today = LocalDate.now(thisClock)

    private val service = ReleaseDateService(clock = thisClock, workingDaysService, iS91DeterminationService)

    @Test
    fun `false if there is no release date`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = null,
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isFalse
    }

    @Test
    fun `false when CRD is yesterday`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = today.minusDays(1),
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isFalse
    }

    @Test
    fun `true when CRD is today`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = today,
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isTrue
    }

    @Test
    fun `true when CRD is 1 day in the future`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = today.plusDays(1),
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isTrue
    }

    @Test
    fun `true when CRD is 2 working days in the future`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = today.plusDays(2),
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isTrue
    }

    @Test
    fun `false when CRD is more than 2 working days in the future`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = today.plusDays(3),
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isFalse
    }

    @Test
    fun `takes the earliest date when ard is earliest`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = today.plusDays(2),
        conditionalReleaseDate = today.plusDays(3),
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isTrue
    }

    @Test
    fun `takes the earliest date when crd is earliest`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = today.plusDays(3),
        conditionalReleaseDate = today.plusDays(2),
      )

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licence)).isTrue
    }
  }

  @Nested
  inner class `Licence is due for early release` {
    val date = LocalDate.of(2024, 4, 4)

    @Test
    fun `missing crd`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = null,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `missing ard`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = date,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `ard and crd the same`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = date,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `ard is one day before crd`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(1),
        conditionalReleaseDate = date,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `ard is two days before crd`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(2),
        conditionalReleaseDate = date,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isTrue
    }

    @Test
    fun `ard is two days before crd when one is a non working day`() {
      val sunday = LocalDate.of(2024, 4, 7)
      val licence = createCrdLicence().copy(
        actualReleaseDate = sunday.minusDays(2),
        conditionalReleaseDate = sunday,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `ard is three days before crd`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date.minusDays(3),
        conditionalReleaseDate = date,
      )

      assertThat(service.isDueForEarlyRelease(licence)).isTrue
    }

    @Test
    fun `crd is one day before ard`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = date.minusDays(1),
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `crd is two days before ard`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = date.minusDays(2),
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }

    @Test
    fun `crd is three days before ard`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = date,
        conditionalReleaseDate = date.minusDays(3),
      )

      assertThat(service.isDueForEarlyRelease(licence)).isFalse
    }
  }

  @Nested
  inner class `Get hard stop date` {
    @Test
    fun `should return null if no release dates provided`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = null,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isNull()
    }

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
    fun `should return null if actual release date is after conditional release date`() {
      val date = LocalDate.parse("2024-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = date.plusDays(1),
        conditionalReleaseDate = date,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `if CRD is on a thursday, then hard stop should be 2 days before`() {
      val thursday = LocalDate.parse("2024-05-16")
      val tuesday = thursday.minusDays(2)

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = thursday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(tuesday)
    }

    @Test
    fun `if CRD is on a thursday and ARD is on the same day, then hard stop remains 2 days before`() {
      val thursday = LocalDate.parse("2024-05-16")
      val tuesday = thursday.minusDays(2)

      val licence = createCrdLicence().copy(
        actualReleaseDate = thursday,
        conditionalReleaseDate = thursday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(tuesday)
    }

    @Test
    fun `if CRD is on a thursday and ARD is on a different day, then there is no hard stop date`() {
      val thursday = LocalDate.parse("2024-05-16")
      val wednesday = thursday.minusDays(1)

      val licence = createCrdLicence().copy(
        actualReleaseDate = wednesday,
        conditionalReleaseDate = thursday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `if CRD is on a saturday, then hard stop should be 2 days before first working day before`() {
      val saturday = LocalDate.parse("2024-05-18")
      val workingDayBefore = saturday.minusDays(1)
      val wednesday = workingDayBefore.minusDays(2)

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = saturday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(wednesday)
    }

    @Test
    fun `if CRD is on a sunday, then hard stop should be 2 days before first working day before`() {
      val sunday = LocalDate.parse("2024-05-19")
      val workingDayBefore = sunday.minusDays(2)
      val wednesday = workingDayBefore.minusDays(2)

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = sunday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(wednesday)
    }

    @Test
    fun `if CRD is on a weekend, and ARD is on the first working day before then hard stop should be 2 days before first working day`() {
      val sunday = LocalDate.parse("2024-05-19")
      val workingDayBefore = sunday.minusDays(2)
      val wednesday = workingDayBefore.minusDays(2)

      val licence = createCrdLicence().copy(
        actualReleaseDate = workingDayBefore,
        conditionalReleaseDate = sunday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(wednesday)
    }

    @Test
    fun `if CRD is on a weekend, and ARD is on the second working day before then hard stop should be null`() {
      val sunday = LocalDate.parse("2024-05-19")
      val thursday = sunday.minusDays(3)

      val licence = createCrdLicence().copy(
        actualReleaseDate = thursday,
        conditionalReleaseDate = sunday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `if CRD is on a Easter Monday, then hard stop should be 6 days before`() {
      // first working day before the monday is the thursday (4 days)
      // two days before the first working day is the Tuesday
      val easterMonday = LocalDate.parse("2018-04-02")
      val tuesdayBefore = LocalDate.parse("2018-03-27")

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = easterMonday,
      )

      val hardStopDate = service.getHardStopDate(licence)
      assertThat(hardStopDate).isEqualTo(tuesdayBefore)
    }
  }

  @Nested
  inner class `Get hard stop warning date` {
    @Test
    fun `should return null if not eligible for hard stop date`() {
      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = null,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `should return 2 days before hard stop date if set`() {
      val friday = LocalDate.parse("2024-05-17")
      val monday = LocalDate.parse("2024-05-13")

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = friday,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(monday)
    }

    @Test
    fun `should return ignore non-working days`() {
      val wednesday = LocalDate.parse("2024-05-15")
      val thursday = LocalDate.parse("2024-05-09")

      val licence = createCrdLicence().copy(
        actualReleaseDate = null,
        conditionalReleaseDate = wednesday,
      )

      val hardStopDate = service.getHardStopWarningDate(licence)
      assertThat(hardStopDate).isEqualTo(thursday)
    }
  }

  @Nested
  inner class `Licence is eligible for early release` {
    @Test
    fun `should return true if ARD or CRD is Friday(2018-01-05)`() {
      val actualReleaseDate = LocalDate.parse("2018-01-05")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue
    }

    @Test
    fun `should return true if ARD or CRD is on Saturday(2018-01-06)`() {
      val actualReleaseDate = LocalDate.parse("2018-01-06")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue
    }

    @Test
    fun `should return true if ARD or CRD is on Sunday(2018-01-07)`() {
      val actualReleaseDate = LocalDate.parse("2018-01-07")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue
    }

    @Test
    fun `should return true if ARD or CRD is on Bank holiday Friday(2018-03-30)`() {
      val actualReleaseDate = LocalDate.parse("2018-03-30")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue
    }

    @Test
    fun `should return true if ARD or CRD is on Bank holiday Monday(2018-04-02)`() {
      val actualReleaseDate = LocalDate.parse("2018-04-02")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isTrue
    }

    @Test
    fun `should return false if ARD or CRD is Thursday (2018-07-05) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-05")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isFalse
    }

    @Test
    fun `should return false if ARD or CRD is Wednesday (2018-07-04) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-04")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isFalse
    }

    @Test
    fun `should return false if ARD or CRD is Monday (2018-07-02) as it is not a bank holiday or weekend`() {
      val actualReleaseDate = LocalDate.parse("2018-07-02")

      val isEligibleForEarlyRelease = service.isEligibleForEarlyRelease(actualReleaseDate)
      assertThat(isEligibleForEarlyRelease).isFalse
    }
  }

  @Nested
  inner class `Licence start date`() {
    @Nested
    inner class `Determine licence start date`() {
      @Test
      fun `returns null if CRD is null`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isNull()
      }

      @Test
      fun `returns CRD if ARD is null and CRD is a working day`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the ARD if it is before the CRD`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 9, 1),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 9, 1))
      }

      @Test
      fun `returns the ARD if the ARD is equal to the CRD`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is after CRD`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns last working day before CRD if the ARD is invalid and CRD is a bank holiday or weekend`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      // Check to make sure it doesn't return workingCrd
      @Test
      fun `returns the ARD when ARD and CRD are both the same non-working day`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 12, 4),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 12, 4))
      }
    }

    @Nested
    inner class `Determine alternative licence start date`() {
      @ParameterizedTest(name = "returns null for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns null if there is no CRD when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 10),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isNull()
      }

      @Test
      fun `returns null if there is no CRD when PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isNull()
      }

      @Test
      fun `returns null if there is no CRD when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isNull()
      }

      @ParameterizedTest(name = "returns CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns the CRD if the ARD is null when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is null when PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is null when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @ParameterizedTest(name = "returns CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns the CRD if the ARD is before the CRD when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is before the CRD when PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is before the CRD when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @ParameterizedTest(name = "returns CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns the CRD if the ARD is after the CRD when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is after the CRD when PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is after the CRD when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @ParameterizedTest(name = "returns last working day before CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns last working day before CRD if CRD is a bank holiday or weekend when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @Test
      fun `returns last working day before CRD if CRD is a bank holiday or weekend when PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @Test
      fun `returns last working day before CRD if CRD is a bank holiday or weekend when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = null,
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @ParameterizedTest(name = "returns last working day before CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns last working day before CRD if CRD is a bank holiday or weekend and the ARD is too early when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 11, 29),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @Test
      fun `returns last working day before CRD if CRD is a bank holiday or weekend and the ARD is too early when PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 11, 29),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @Test
      fun `returns last working day before CRD if CRD is a bank holiday or weekend and the ARD is too early when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 11, 29),
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @ParameterizedTest(name = "returns ARD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns ARD when the CRD and ARD are the same non-working day and the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 4),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 12, 4))
      }

      @Test
      fun `returns ARD when the CRD and ARD are the same non-working day and PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 4),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 12, 4))
      }

      @Test
      fun `returns ARD when the CRD and ARD are the same non-working day and it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 4),
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 12, 4))
      }

      @ParameterizedTest(name = "returns ARD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns ARD when the CRD is a non-working day and the ARD is an earlier non-working day and the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 3),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 12, 3))
      }

      @Test
      fun `returns ARD when the CRD is a non-working day and the ARD is an earlier non-working day and PED is in the past`() {
        val nomisRecord = prisonerSearchResult().copy(
          paroleEligibilityDate = LocalDate.of(2020, 1, 1),
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 3),
        )

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 12, 3))
      }

      @Test
      fun `returns ARD when the CRD is a non-working day and the ARD is an earlier non-working day and it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 3),
        )

        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(listOf(123456))

        assertThat(service.getLicenceStartDate(nomisRecord)).isEqualTo(LocalDate.of(2021, 12, 3))
      }
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
