package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
    )

  @BeforeEach
  fun reset() {
    reset(bankHolidayService)
    whenever(bankHolidayService.getBankHolidaysForEnglandAndWales()).thenReturn(bankHolidays)
  }

  private fun getEarliestDate(actualReleaseDate: LocalDate): LocalDate {
    return service.getEarliestReleaseDate(actualReleaseDate)
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-01-02) if ARD or CRD is Friday(2018-01-05) and day they would normally be released is Friday`() {
    val actualReleaseDate = LocalDate.parse("2018-01-05")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-01-02) if ARD or CRD is on Saturday(2018-01-06) and day they would normally be released is Friday`() {
    val actualReleaseDate = LocalDate.parse("2018-01-06")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-01-02) if ARD or CRD is on Sunday(2018-01-07) and day they would normally be released is Friday`() {
    val actualReleaseDate = LocalDate.parse("2018-01-07")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-01-02"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-03-27) if ARD or CRD is on Bank holiday Friday(2018-03-30) and day they would normally be released is Thursday`() {
    val actualReleaseDate = LocalDate.parse("2018-03-30")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-04-27) if ARD or CRD is on Bank holiday Monday(2018-04-02) and day they would normally be released is Friday`() {
    val actualReleaseDate = LocalDate.parse("2018-04-02")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-05-29) if ARD or CRD is on Bank holiday Monday(2018-06-04) Friday(2018-06-01) before also bank holiday and day they would normally be released is Thursday`() {
    val actualReleaseDate = LocalDate.parse("2018-06-04")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-05-29"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-11-27) if ARD or CRD is on Monday(2018-12-03) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
    val actualReleaseDate = LocalDate.parse("2018-12-03")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
  }

  @Test
  fun `earliestReleaseDate should return Tuesday(2018-11-27) if ARD or CRD is on Tuesday(2018-12-04) with Consecutive Bank holiday Monday(2018-12-03) and Tuesday(2018-12-04) before also bank holiday and day they would normally be released is Friday`() {
    val actualReleaseDate = LocalDate.parse("2018-12-04")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-11-27"))
  }

  @Test
  fun `earliestReleaseDate should return Wednesday(2018-08-01) if ARD or CRD is on Bank holiday Tuesday(2018-08-07) and day they would normally be released is Monday`() {
    val actualReleaseDate = LocalDate.parse("2018-08-07")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-08-01"))
  }

  @Test
  fun `earliestReleaseDate should return Thursday(2018-09-27) if ARD or CRD is on Bank holiday Wednesday(2018-10-03) and day they would normally be released is Tuesday`() {
    val actualReleaseDate = LocalDate.parse("2018-10-03")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-09-27"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-03-27) if ARD is (2018-03-30) third working day before CRD`() {
    val actualReleaseDate = LocalDate.parse("2018-03-30")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-03-27) if ARD is (2018-04-02) third working day before CRD`() {
    val actualReleaseDate = LocalDate.parse("2018-04-02")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-03-27"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-04-30) if ARD is (2018-05-07) and (2018-05-02) is bank holiday`() {
    val actualReleaseDate = LocalDate.parse("2018-05-07")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-04-30"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if ARD is (2018-07-06) third working day before CRD`() {
    val actualReleaseDate = LocalDate.parse("2018-07-06")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if ARD is (2018-07-07) as it is not a bank holiday or weekend`() {
    val actualReleaseDate = LocalDate.parse("2018-07-07")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if ARD is (2018-07-08) as it is not a bank holiday or weekend`() {
    val actualReleaseDate = LocalDate.parse("2018-07-08")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `earliestReleaseDate should return (2018-07-03) if CRD is (2018-07-08) as it is not a bank holiday or weekend`() {
    val actualReleaseDate = LocalDate.parse("2018-07-08")

    val earliestPossibleReleaseDate = getEarliestDate(actualReleaseDate)
    assertThat(earliestPossibleReleaseDate).isEqualTo(LocalDate.parse("2018-07-03"))
  }

  @Test
  fun `isLateAllocationWarningRequired should return false if the difference between release date and modified date is equal to 6 days`() {
    assertFalse(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-20T00:01", formatter)))
  }

  @Test
  fun `isLateAllocationWarningRequired should return true if the difference between release date and modified date is equal to 5 days`() {
    assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-17T00:01", formatter)))
  }

  @Test
  fun `isLateAllocationWarningRequired should return true if the difference between release date and modified date is 4 days`() {
    assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-16")))
  }
  
  @Test
  fun `isLateAllocationWarningRequired should return false if the release date is before modified date`() {
    assertFalse(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-01")))
  }

  @Test
  fun `isLateAllocationWarningRequired should return false if the release date is on modified date`() {
    assertFalse(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-10")))
  }

  @Test
  fun `isLateAllocationWarningRequired should return true if the release date is after modified date`() {
    assertTrue(service.isLateAllocationWarningRequired(LocalDate.parse("2023-11-13")))
  }

  @Test
  fun `should return false1 if releaseDate is null`() {
    assertFalse(service.isLateAllocationWarningRequired(null))
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
    val clock: Clock = Clock.fixed(Instant.parse("2023-11-10T00:00:00Z"), ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm]")
  }
}
