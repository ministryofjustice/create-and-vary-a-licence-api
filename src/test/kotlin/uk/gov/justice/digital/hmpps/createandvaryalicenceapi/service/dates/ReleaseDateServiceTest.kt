package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.BankHolidayService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
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
      isTimeServedEnabled = true,
      timeServedEnabledPrisons = listOf("MDI"),
    )

  @BeforeEach
  fun reset() {
    reset(bankHolidayService)
    whenever(bankHolidayService.getBankHolidaysForEnglandAndWales()).thenReturn(bankHolidays)
  }

  private fun getEarliestDate(
    licenceStartDate: LocalDate?,
    homeDetentionCurfewActualDate: LocalDate? = null,
  ): LocalDate? = service.getEarliestReleaseDate(
    object : SentenceDateHolder {
      override val licenceStartDate: LocalDate? = licenceStartDate
      override val sentenceStartDate: LocalDate? = null
      override val conditionalReleaseDate: LocalDate? = null
      override val actualReleaseDate: LocalDate? = null
      override val homeDetentionCurfewActualDate: LocalDate? = homeDetentionCurfewActualDate
      override val postRecallReleaseDate: LocalDate? = null
    },
  )

  fun getCutOffDateForLicenceTimeOut(now: Clock? = null): LocalDate = workingDaysService.workingDaysAfter(LocalDate.now(now)).take(2).last()

  @Nested
  inner class `Earliest release date` {
    @Test
    fun `should return Tuesday(2018-01-02) if ARD or CRD is Friday(2018-01-05) and day they would normally be released is Friday`() {
      val licenceStartDate = LocalDate.parse("2018-01-05")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
    }

    @Test
    fun `should return Tuesday(2018-01-02) if ARD or CRD is on Saturday(2018-01-06) and day they would normally be released is Friday`() {
      val licenceStartDate = LocalDate.parse("2018-01-06")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
    }

    @Test
    fun `should return Tuesday(2018-01-02) if ARD or CRD is on Sunday(2018-01-07) and day they would normally be released is Friday`() {
      val licenceStartDate = LocalDate.parse("2018-01-07")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
    }

    @Test
    fun `should return Tuesday(2018-03-27) if ARD or CRD is on Bank holiday Friday(2018-03-30) and day they would normally be released is Thursday`() {
      val licenceStartDate = LocalDate.parse("2018-03-30")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return Tuesday(2018-04-27) if ARD or CRD is on Bank holiday Monday(2018-04-02) and day they would normally be released is Friday`() {
      val licenceStartDate = LocalDate.parse("2018-04-02")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return Tuesday(2018-05-29) if ARD or CRD is on Bank holiday Monday(2018-06-04) Friday(2018-06-01) before also bank holiday and day they would normally be released is Thursday`() {
      val licenceStartDate = LocalDate.parse("2018-06-04")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-05-29"))
    }

    @Test
    fun `should return Tuesday(2018-11-27) if ARD or CRD is on Monday(2018-12-03) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
      val licenceStartDate = LocalDate.parse("2018-12-03")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
    }

    @Test
    fun `should return Tuesday(2018-11-27) if ARD or CRD is on Tuesday(2018-12-04) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
      val licenceStartDate = LocalDate.parse("2018-12-04")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
    }

    @Test
    fun `should return Wednesday(2018-08-01) if ARD or CRD is on Bank holiday Tuesday(2018-08-07) and day they would normally be released is Monday`() {
      val licenceStartDate = LocalDate.parse("2018-08-07")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-08-01"))
    }

    @Test
    fun `should return Thursday(2018-09-27) if ARD or CRD is on Bank holiday Wednesday(2018-10-03) and day they would normally be released is Tuesday`() {
      val licenceStartDate = LocalDate.parse("2018-10-03")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-09-27"))
    }

    @Test
    fun `should return (2018-03-27) if ARD is (2018-03-30) third working day before CRD`() {
      val licenceStartDate = LocalDate.parse("2018-03-30")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return (2018-03-27) if ARD is (2018-04-02) third working day before CRD`() {
      val licenceStartDate = LocalDate.parse("2018-04-02")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
    }

    @Test
    fun `should return (2018-04-30) if ARD is (2018-05-07) and (2018-05-02) is bank holiday`() {
      val licenceStartDate = LocalDate.parse("2018-05-07")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-04-30"))
    }

    @Test
    fun `should return (2018-07-03) if ARD is (2018-07-06) third working day before CRD`() {
      val licenceStartDate = LocalDate.parse("2018-07-06")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }

    @Test
    fun `should return (2018-07-03) if LSD is (2018-07-07) as it is not a bank holiday or weekend`() {
      val licenceStartDate = LocalDate.parse("2018-07-07")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }

    @Test
    fun `should return (2018-07-03) if LSD is (2018-07-08) as it is not a bank holiday or weekend`() {
      val licenceStartDate = LocalDate.parse("2018-07-08")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
    }

    @Test
    fun `should return null if the LSD is null`() {
      val licenceStartDate = null

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate)
      assertThat(earliestPossibleReleaseDate).isNull()
    }

    @Test
    fun `should return (2018-07-08) if LSD and HDCAD are (2018-07-08)`() {
      val licenceStartDate = LocalDate.parse("2018-07-08")
      val homeDetentionCurfewActualDate = LocalDate.parse("2018-07-08")

      val earliestPossibleReleaseDate = getEarliestDate(licenceStartDate, homeDetentionCurfewActualDate)
      assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-08"))
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
    fun `returns false if licence start date is absent`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        licenceStartDate = null,
      )

      assertThat(service.isInHardStopPeriod(licence.licenceStartDate, licence.kind, now)).isFalse
    }

    @Test
    fun `licence is still in hard stop period on release day`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        licenceStartDate = LocalDate.now(now),
      )

      assertThat(service.isInHardStopPeriod(licence.licenceStartDate, licence.kind, now)).isTrue
    }

    @Test
    fun `licence is not in hard stop period after release`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createCrdLicence().copy(
        licenceStartDate = LocalDate.now(now).minusDays(1),
      )

      assertThat(service.isInHardStopPeriod(licence.licenceStartDate, licence.kind, now)).isFalse
    }

    @Test
    fun `licence is in hard stop period if LSD is at the timeout threshold`() {
      val now = createClock("2018-03-12T00:00:00Z")
      val cutOff = getCutOffDateForLicenceTimeOut(now)

      val licence = createCrdLicence().copy(
        licenceStartDate = cutOff,
      )

      assertThat(service.isInHardStopPeriod(licence.licenceStartDate, licence.kind, now)).isTrue
    }

    @Test
    fun `returns false if it is a time served licence`() {
      val now = createClock("2018-03-12T00:00:00Z")

      val licence = createTimeServedLicence()

      assertThat(service.isInHardStopPeriod(licence.licenceStartDate, licence.kind, now)).isFalse
    }
  }

  @Nested
  inner class `Licence is due to be released in the next two days` {
    val thisClock = createClock("2024-04-22T00:00:00Z")
    val today = LocalDate.now(thisClock)

    private val service = ReleaseDateService(clock = thisClock, workingDaysService, iS91DeterminationService)

    @Test
    fun `false if there is no release date`() {
      val licenceStartDate = null
      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate)).isFalse
    }

    @Test
    fun `false when LSD is yesterday`() {
      val licenceStartDate = today.minusDays(1)
      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate)).isFalse
    }

    @Test
    fun `true when LSD is today`() {
      val licenceStartDate = today
      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate)).isTrue
    }

    @Test
    fun `true when LSD is 1 day in the future`() {
      val licenceStartDate = today.plusDays(1)

      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate)).isTrue
    }

    @Test
    fun `true when LSD is 2 working days in the future`() {
      val licenceStartDate = today.plusDays(2)
      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate)).isTrue
    }

    @Test
    fun `false when LSD is more than 2 working days in the future`() {
      val licenceStartDate = today.plusDays(3)
      assertThat(service.isDueToBeReleasedInTheNextTwoWorkingDays(licenceStartDate)).isFalse
    }
  }

  @Nested
  inner class `Get hard stop date` {
    @Test
    fun `should return null if no LSD provided`() {
      val licence = createCrdLicence().copy(
        licenceStartDate = null,
      )

      val hardStopDate = service.getHardStopDate(licence.licenceStartDate, licence.kind)
      assertThat(hardStopDate).isNull()
    }

    @Test
    fun `if LSD is on a thursday, then hard stop should be 2 days before`() {
      val thursday = LocalDate.parse("2024-05-16")
      val tuesday = thursday.minusDays(2)

      val licence = createCrdLicence().copy(
        licenceStartDate = thursday,
      )

      val hardStopDate = service.getHardStopDate(licence.licenceStartDate, licence.kind)
      assertThat(hardStopDate).isEqualTo(tuesday)
    }

    @Test
    fun `if LSD is on a saturday, then hard stop should be 2 days before first working day before`() {
      val saturday = LocalDate.parse("2024-05-18")
      val workingDayBefore = saturday.minusDays(1)
      val wednesday = workingDayBefore.minusDays(2)

      val licence = createCrdLicence().copy(
        licenceStartDate = saturday,
      )

      val hardStopDate = service.getHardStopDate(licence.licenceStartDate, licence.kind)
      assertThat(hardStopDate).isEqualTo(wednesday)
    }

    @Test
    fun `if LSD is on a sunday, then hard stop should be 2 days before first working day before`() {
      val sunday = LocalDate.parse("2024-05-19")
      val workingDayBefore = sunday.minusDays(2)
      val wednesday = workingDayBefore.minusDays(2)

      val licence = createCrdLicence().copy(
        licenceStartDate = sunday,
      )

      val hardStopDate = service.getHardStopDate(licence.licenceStartDate, licence.kind)
      assertThat(hardStopDate).isEqualTo(wednesday)
    }

    @Test
    fun `if LSD is on a Easter Monday, then hard stop should be 6 days before`() {
      // first working day before the monday is the thursday (4 days)
      // two days before the first working day is the Tuesday
      val easterMonday = LocalDate.parse("2018-04-02")
      val tuesdayBefore = easterMonday.minusDays(6)

      val licence = createCrdLicence().copy(
        licenceStartDate = easterMonday,
      )

      val hardStopDate = service.getHardStopDate(licence.licenceStartDate, licence.kind)
      assertThat(hardStopDate).isEqualTo(tuesdayBefore)
    }

    @Test
    fun `should return null if it is a time served licence`() {
      val licence = createTimeServedLicence()

      val hardStopDate = service.getHardStopDate(licence.licenceStartDate, licence.kind)
      assertThat(hardStopDate).isNull()
    }
  }

  @Nested
  inner class `Get hard stop warning date` {

    @Test
    fun `should return 2 days before hard stop date if set`() {
      val friday = LocalDate.parse("2024-05-17")
      val monday = LocalDate.parse("2024-05-13")
      val licence = createCrdLicence()

      val hardStopDate = service.getHardStopWarningDate(friday, licence.kind)
      assertThat(hardStopDate).isEqualTo(monday)
    }

    @Test
    fun `should return ignore non-working days`() {
      val wednesday = LocalDate.parse("2024-05-15")
      val thursday = LocalDate.parse("2024-05-09")
      val licence = createCrdLicence()

      val hardStopDate = service.getHardStopWarningDate(wednesday, licence.kind)
      assertThat(hardStopDate).isEqualTo(thursday)
    }

    @Test
    fun `should return null for a time served licence`() {
      val friday = LocalDate.parse("2024-05-17")
      val monday = LocalDate.parse("2024-05-13")
      val licence = createTimeServedLicence()

      val hardStopDate = service.getHardStopWarningDate(friday, licence.kind)
      assertThat(hardStopDate).isNull()
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
  inner class `Licence start date` {
    @Nested
    inner class `Determine licence start date` {
      @Test
      fun `returns null if CRD is null`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isNull()
      }

      @Test
      fun `returns CRD if ARD is null and CRD is a working day`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the ARD if it is before the CRD`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 9, 1),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 9, 1))
      }

      @Test
      fun `returns the ARD if the ARD is equal to the CRD`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 22),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is after CRD`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns last working day before CRD if the ARD is invalid and CRD is a bank holiday or weekend`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      // Check to make sure it doesn't return workingCrd
      @Test
      fun `returns the ARD when ARD and CRD are both the same non-working day`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 12, 4),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2018, 12, 4))
      }
    }

    @Nested
    inner class `Determine alternative licence start date` {
      @ParameterizedTest(name = "returns null for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns null if there is no CRD when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 10),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isNull()
      }

      @Test
      fun `returns null if there is no CRD when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isNull()
      }

      @ParameterizedTest(name = "returns CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns the CRD if the ARD is null when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is null when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = null,
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @ParameterizedTest(name = "returns CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns the CRD if the ARD is before the CRD when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is before the CRD when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 21),
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @ParameterizedTest(name = "returns CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns the CRD if the ARD is after the CRD when the legal status is one of note`(legalStatus: String) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @Test
      fun `returns the CRD if the ARD is after the CRD when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 10, 22),
          confirmedReleaseDate = LocalDate.of(2021, 10, 23),
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 10, 22))
      }

      @ParameterizedTest(name = "returns last working day before CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns last working day before CRD if CRD is a bank holiday or weekend when the legal status is one of note`(
        legalStatus: String,
      ) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @Test
      fun `returns last working day before CRD if CRD is a bank holiday or weekend when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = null,
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @ParameterizedTest(name = "returns last working day before CRD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns last working day before CRD if CRD is a bank holiday or weekend and the ARD is too early when the legal status is one of note`(
        legalStatus: String,
      ) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 11, 29),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @Test
      fun `returns last working day before CRD if CRD is a bank holiday or weekend and the ARD is too early when it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2018, 12, 4),
          confirmedReleaseDate = LocalDate.of(2018, 11, 29),
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2018, 11, 30))
      }

      @ParameterizedTest(name = "returns ARD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns ARD when the CRD and ARD are the same non-working day and the legal status is one of note`(
        legalStatus: String,
      ) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 4),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 12, 4))
      }

      @Test
      fun `returns ARD when the CRD and ARD are the same non-working day and it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 4),
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 12, 4))
      }

      @ParameterizedTest(name = "returns ARD for {0}")
      @ValueSource(strings = ["IMMIGRATION_DETAINEE", "REMAND", "CONVICTED_UNSENTENCED"])
      fun `returns ARD when the CRD is a non-working day and the ARD is an earlier non-working day and the legal status is one of note`(
        legalStatus: String,
      ) {
        val nomisRecord = prisonerSearchResult().copy(
          legalStatus = legalStatus,
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 3),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 12, 3))
      }

      @Test
      fun `returns ARD when the CRD is a non-working day and the ARD is an earlier non-working day and it is an IS91 case`() {
        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = LocalDate.of(2021, 12, 4),
          confirmedReleaseDate = LocalDate.of(2021, 12, 3),
        )

        whenItsAnIS91Case(nomisRecord)

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.CRD)).isEqualTo(LocalDate.of(2021, 12, 3))
      }
    }

    @Nested
    inner class `Calculated PRRD licence start date` {
      @Test
      fun `returns null if PRRD is null`() {
        val nomisRecord = prisonerSearchResult().copy(
          postRecallReleaseDate = null,
          confirmedReleaseDate = LocalDate.of(2024, 10, 22),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isNull()
      }

      @Test
      fun `returns PRRD if ARD is null and PRRD is a working day`() {
        val prrd = LocalDate.of(2024, 10, 22)

        val nomisRecord = prisonerSearchResult().copy(
          postRecallReleaseDate = prrd,
          confirmedReleaseDate = null,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(prrd)
      }

      @Test
      fun `returns the ARD if it is before the PRRD and after the CRD`() {
        val prrd = LocalDate.of(2024, 10, 22)
        val ard = prrd.minusDays(2)

        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = prrd.minusYears(3),
          confirmedReleaseDate = ard,
          postRecallReleaseDate = prrd,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(ard)
      }

      @Test
      fun `returns the ARD if it is before the PRRD and the CRD is null`() {
        val prrd = LocalDate.of(2024, 10, 22)
        val ard = prrd.minusDays(2)

        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = null,
          confirmedReleaseDate = ard,
          postRecallReleaseDate = prrd,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(ard)
      }

      @Test
      fun `returns the ARD if the ARD is equal to the PRRD`() {
        val prrdAndArd = LocalDate.of(2024, 10, 22)

        val nomisRecord = prisonerSearchResult().copy(
          postRecallReleaseDate = prrdAndArd,
          confirmedReleaseDate = prrdAndArd,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(prrdAndArd)
      }

      @Test
      fun `returns the PRRD if the ARD is after PRRD`() {
        val prrd = LocalDate.of(2024, 10, 22)

        val nomisRecord = prisonerSearchResult().copy(
          postRecallReleaseDate = prrd,
          confirmedReleaseDate = prrd.plusDays(1),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(prrd)
      }

      @Test
      fun `returns last working day before PRRD if the ARD is after PRRD and PRRD is a bank holiday or weekend`() {
        val prrd = LocalDate.of(2018, 12, 4)
        val lastWorkingDayBeforePrrd = LocalDate.of(2018, 11, 30)

        val nomisRecord = prisonerSearchResult().copy(
          postRecallReleaseDate = prrd,
          confirmedReleaseDate = prrd.plusDays(10),
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(lastWorkingDayBeforePrrd)
      }

      // Check to make sure it doesn't return last working day
      @Test
      fun `returns the ARD when ARD and PRRD are both the same non-working day`() {
        val prrdAndArd = LocalDate.of(2018, 12, 4)

        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = null,
          postRecallReleaseDate = prrdAndArd,
          confirmedReleaseDate = prrdAndArd,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(prrdAndArd)
      }

      @Test
      fun `returns the PRRD if the ARD is before the CRD`() {
        val prrd = LocalDate.of(2024, 10, 22)
        val crd = prrd.minusYears(3)
        val ard = crd.minusDays(1)

        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = crd,
          postRecallReleaseDate = prrd,
          confirmedReleaseDate = ard,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(prrd)
      }

      @Test
      fun `returns the PRRD if the ARD is equal to the CRD`() {
        val prrd = LocalDate.of(2024, 10, 22)
        val crdAndArd = prrd.minusYears(3)

        val nomisRecord = prisonerSearchResult().copy(
          conditionalReleaseDate = crdAndArd,
          postRecallReleaseDate = prrd,
          confirmedReleaseDate = crdAndArd,
        )

        assertThat(service.getLicenceStartDate(nomisRecord, LicenceKind.PRRD)).isEqualTo(prrd)
      }
    }

    @Nested
    inner class `getLicenceStartDates - bulk calculations` {
      private val cases = listOf(
        // IS91 Legal status
        prisonerSearchResult().copy(
          prisonerNumber = "I9123SL",
          bookingId = "234567",
          legalStatus = "IMMIGRATION_DETAINEE",
          conditionalReleaseDate = LocalDate.of(2021, 10, 21),
          confirmedReleaseDate = LocalDate.of(2021, 10, 20),
        ),
        // Remand legal status
        prisonerSearchResult().copy(
          prisonerNumber = "R1234MD",
          bookingId = "345678",
          legalStatus = "REMAND",
          conditionalReleaseDate = LocalDate.of(2021, 10, 21),
          confirmedReleaseDate = LocalDate.of(2021, 10, 20),
        ),
        // Convicted unsentenced legal status
        prisonerSearchResult().copy(
          prisonerNumber = "C1234NV",
          bookingId = "456789",
          legalStatus = "CONVICTED_UNSENTENCED",
          conditionalReleaseDate = LocalDate.of(2021, 10, 21),
          confirmedReleaseDate = LocalDate.of(2021, 10, 20),
        ),
        // IS91 booking ID
        prisonerSearchResult().copy(
          prisonerNumber = "I9123SB",
          bookingId = "567890",
          conditionalReleaseDate = LocalDate.of(2021, 10, 21),
          confirmedReleaseDate = LocalDate.of(2021, 10, 20),
        ),
        // Standard case
        prisonerSearchResult().copy(
          prisonerNumber = "S1234TD",
          bookingId = "678901",
          conditionalReleaseDate = LocalDate.of(2021, 10, 21),
          confirmedReleaseDate = LocalDate.of(2021, 10, 20),
        ),
        // Ineligible case
        prisonerSearchResult().copy(
          prisonerNumber = "I1234GB",
          bookingId = "789012",
          conditionalReleaseDate = LocalDate.of(2021, 10, 21),
          confirmedReleaseDate = LocalDate.of(2021, 10, 20),
        ),
      )

      private val kinds = mapOf(
        "I9123SL" to LicenceKind.CRD,
        "R1234MD" to LicenceKind.CRD,
        "C1234NV" to LicenceKind.CRD,
        "I9123SB" to LicenceKind.CRD,
        "S1234TD" to LicenceKind.CRD,
        "I1234GB" to null,
      )

      @Test
      fun `returns a map of nomis ID to licence start date`() {
        whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(any())).thenReturn(listOf(567890))
        val expectedResponse = mapOf(
          "I9123SL" to LocalDate.of(2021, 10, 21),
          "R1234MD" to LocalDate.of(2021, 10, 21),
          "C1234NV" to LocalDate.of(2021, 10, 21),
          "I9123SB" to LocalDate.of(2021, 10, 21),
          "S1234TD" to LocalDate.of(2021, 10, 20),
          "I1234GB" to null,
        )
        assertThat(service.getLicenceStartDates(cases, kinds)).isEqualTo(expectedResponse)
      }
    }

    @Test
    fun `returns null if the licence kind is null`() {
      val nomisRecord = prisonerSearchResult().copy(
        conditionalReleaseDate = LocalDate.of(2021, 12, 4),
        confirmedReleaseDate = LocalDate.of(2021, 12, 3),
      )

      whenItsAnIS91Case(nomisRecord)

      assertThat(service.getLicenceStartDate(nomisRecord, null)).isNull()
    }
  }

  private fun whenItsAnIS91Case(nomisRecord: PrisonerSearchPrisoner) {
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(
      listOf(
        nomisRecord.bookingId!!.toLong(),
      ),
    )
  }

  private fun whenItsNotAnIS91Case(nomisRecord: PrisonerSearchPrisoner) {
    whenever(iS91DeterminationService.getIS91AndExtraditionBookingIds(listOf(nomisRecord))).thenReturn(
      emptyList(),
    )
  }

  @Nested
  inner class `Get hard stop kind` {
    val thisClock = createClock("2024-04-22T00:00:00Z")
    val today = LocalDate.now(thisClock)
    val cutOff = getCutOffDateForLicenceTimeOut(thisClock)
    val prisonCode = "MDI"

    @Test
    fun `returns HARD_STOP when is in Hard Stop Period`() {
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today.minusDays(1),
        confirmedReleaseDate = today.minusDays(1),
        conditionalReleaseDate = today.minusDays(1),
        conditionalReleaseDateOverrideDate = today.minusDays(1),
      )

      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }

    @Test
    fun `returns null when all dates are null`() {
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = null,
        confirmedReleaseDate = null,
        conditionalReleaseDate = null,
        conditionalReleaseDateOverrideDate = null,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(null), prisonCode, thisClock)
      assertNull(result)
    }

    @Test
    fun `returns TIME_SERVED when all dates are today and override date is null`() {
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today,
        confirmedReleaseDate = today,
        conditionalReleaseDate = today,
        conditionalReleaseDateOverrideDate = null,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.TIME_SERVED, result)
    }

    @Test
    fun `returns HARD_STOP when sentenceStartDate is not today`() {
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today.minusDays(1),
        confirmedReleaseDate = today,
        conditionalReleaseDate = today,
        conditionalReleaseDateOverrideDate = null,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }

    @Test
    fun `returns HARD_STOP when confirmedReleaseDate is not today`() {
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today,
        confirmedReleaseDate = today.minusDays(1),
        conditionalReleaseDate = today,
        conditionalReleaseDateOverrideDate = null,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }

    @Test
    fun `returns HARD_STOP when neither conditionalReleaseDateOverrideDate nor conditionalReleaseDate is today`() {
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today,
        confirmedReleaseDate = today,
        conditionalReleaseDate = today.minusDays(1),
        conditionalReleaseDateOverrideDate = today.minusDays(1),
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }

    @Test
    fun `returns HARD_STOP when all dates are today and override date is today except conditional release date and isTimeServedEnabled falg is disabled`() {
      val service =
        ReleaseDateService(clock = thisClock, workingDaysService, iS91DeterminationService, isTimeServedEnabled = false)
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today,
        confirmedReleaseDate = today,
        conditionalReleaseDate = today.minusDays(1),
        conditionalReleaseDateOverrideDate = today,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }

    @Test
    fun `returns HARD_STOP when all dates are today and isTimeServedEnabled flag is enabled but prison is not in the enabled list`() {
      val service =
        ReleaseDateService(clock = thisClock, workingDaysService, iS91DeterminationService, isTimeServedEnabled = true, timeServedEnabledPrisons = listOf("LEI", "BXI"))
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today,
        confirmedReleaseDate = today,
        conditionalReleaseDate = today,
        conditionalReleaseDateOverrideDate = today,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }

    @Test
    fun `returns HARD_STOP when all dates are today and isTimeServedEnabled falg enabled but timeServedEnabledPrisons is null`() {
      val service =
        ReleaseDateService(clock = thisClock, workingDaysService, iS91DeterminationService, isTimeServedEnabled = true, timeServedEnabledPrisons = null)
      val nomisRecord = prisonerSearchResult().copy(
        sentenceStartDate = today,
        confirmedReleaseDate = today,
        conditionalReleaseDate = today,
        conditionalReleaseDateOverrideDate = today,
      )
      val result = service.getHardStopKind(nomisRecord.toSentenceDateHolder(cutOff), prisonCode, thisClock)
      assertEquals(LicenceKind.HARD_STOP, result)
    }
  }

  @Nested
  inner class `isReleaseAtLed` {

    @Test
    fun `returns false if release date is null`() {
      val licenceExpiryDate = LocalDate.of(2022, 10, 10)
      assertThat(service.isReleaseAtLed(null, licenceExpiryDate)).isFalse()
    }

    @Test
    fun `returns false if licence expiry date is null`() {
      val releaseDate = LocalDate.of(2022, 10, 10)
      assertThat(service.isReleaseAtLed(releaseDate, null)).isFalse()
    }

    @Test
    fun `returns false if both dates date are null`() {
      assertThat(service.isReleaseAtLed(null, null)).isFalse()
    }

    @Test
    fun `returns true if release date is equal to LED`() {
      val licenceExpiryDate = LocalDate.of(2022, 10, 10)
      val releaseDate = LocalDate.of(2022, 10, 10)
      assertThat(service.isReleaseAtLed(releaseDate, licenceExpiryDate)).isTrue()
    }

    @Test
    fun `returns false if release date is equal to last working day before LED`() {
      // Stubbed non-working day
      val licenceExpiryDate = LocalDate.of(2018, 12, 4)

      val releaseDate = LocalDate.of(2018, 11, 30)
      assertThat(service.isReleaseAtLed(releaseDate, licenceExpiryDate)).isFalse()
    }

    @Test
    fun `returns false if release date is not equal to LED`() {
      val licenceExpiryDate = LocalDate.of(2022, 10, 10)
      val releaseDate = LocalDate.of(2022, 10, 5)
      assertThat(service.isReleaseAtLed(releaseDate, licenceExpiryDate)).isFalse()
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
