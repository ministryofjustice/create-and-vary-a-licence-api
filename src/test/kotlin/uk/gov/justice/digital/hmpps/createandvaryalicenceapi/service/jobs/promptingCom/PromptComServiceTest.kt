package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PromptComServiceTest {

  private val promptComListBuilder = mock<PromptComListBuilder>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val promptComService = PromptComService(promptComListBuilder, prisonerSearchApiClient)

  @BeforeEach
  fun reset() = reset(promptComListBuilder, prisonerSearchApiClient)

  @Test
  fun noRecords() {
    whenever(prisonerSearchApiClient.getAllByReleaseDate(start, end)).thenReturn(emptyList())

    val emails = promptComService.runJob(clock)

    assertThat(emails).isEmpty()
  }

  @Test
  fun recordsProcessed() {
    whenever(prisonerSearchApiClient.getAllByReleaseDate(start, end)).thenReturn(cases)

    whenever(promptComListBuilder.excludeIneligibleCases(any())).thenReturn(cases)

    whenever(promptComListBuilder.excludeInflightLicences(any())).thenReturn(cases)

    whenever(promptComListBuilder.excludePrisonersWithHdc(any())).thenReturn(cases)

    whenever(promptComListBuilder.enrichWithDeliusData(any())).thenReturn(casesWithDeliusData)

    whenever(promptComListBuilder.enrichWithComEmail(any())).thenReturn(casesWithEmails)

    whenever(promptComListBuilder.enrichWithLicenceStartDates(any())).thenReturn(casesWithEmailsAndLocalDates)

    whenever(promptComListBuilder.excludeOutOfRangeDates(any(), eq(start), eq(end))).thenReturn(
      casesWithEmailsAndLocalDates,
    )

    whenever(promptComListBuilder.excludeInHardStop(any())).thenReturn(casesWithEmailsAndLocalDates)

    whenever(promptComListBuilder.buildEmailsToSend(any())).thenReturn(listOf(com))

    val emails = promptComService.runJob(clock)

    assertThat(emails).containsExactly(com)

    inOrder(prisonerSearchApiClient, promptComListBuilder) {
      verify(prisonerSearchApiClient).getAllByReleaseDate(start, end)

      verify(promptComListBuilder).excludeIneligibleCases(cases)

      verify(promptComListBuilder).excludeInflightLicences(cases)

      verify(promptComListBuilder).excludePrisonersWithHdc(cases)

      verify(promptComListBuilder).enrichWithDeliusData(cases)

      verify(promptComListBuilder).enrichWithComEmail(casesWithDeliusData)

      verify(promptComListBuilder).enrichWithLicenceStartDates(casesWithEmails)

      verify(promptComListBuilder).excludeOutOfRangeDates(casesWithEmailsAndLocalDates, start, end)

      verify(promptComListBuilder).excludeInHardStop(casesWithEmailsAndLocalDates)

      verify(promptComListBuilder).buildEmailsToSend(casesWithEmailsAndLocalDates)
    }
  }

  companion object {
    val clock = Clock.fixed(
      Instant.parse("2025-01-29T10:15:30.00Z"),
      ZoneId.of("Europe/London"),
    )

    val start = LocalDate.parse("2025-01-27")
    val end = LocalDate.parse("2025-02-24")

    val cases = listOf(TestData.prisonerSearchResult())
    val casesWithDeliusData = listOf(TestData.promptCase())
    val casesWithEmails = casesWithDeliusData.map { it to "com@email.com" }
    val casesWithEmailsAndLocalDates = casesWithEmails.map { it to LocalDate.of(2022, 1, 2) }

    val com = Com(
      email = "com@email.com",
      comName = "com name",
      subjects = listOf(
        Subject(
          prisonerNumber = "A1234AA",
          crn = "crn",
          name = "name",
          releaseDate = LocalDate.of(2022, 1, 2),
        ),
      ),
    )
  }

  @Nested
  inner class TimeRange {

    @ParameterizedTest
    @CsvSource(
      value = [
        "2025-01-29T10:15:30.00Z, 2025-01-27, 2025-02-24",
        "2025-01-27T00:00:00.01Z, 2025-01-27, 2025-02-24",
        "2025-01-26T23:59:59.999Z, 2025-01-20, 2025-02-17",

        // Daylight saving time - 30th March 2025
        "2025-03-30T23:59:59.999Z, 2025-03-31, 2025-04-28",
        "2025-03-30T22:59:59.999Z, 2025-03-24, 2025-04-21",

        // Daylight saving ends - 26th October 2025
        "2025-10-26T00:59:59.999Z, 2025-10-20, 2025-11-17",
        "2025-10-27T00:00:00.0001Z, 2025-10-27, 2025-11-24",

        // Year start:
        "2025-01-02T05:00:00.00Z, 2024-12-30, 2025-01-27",
      ],
    )
    fun check(now: String, start: String, end: String) {
      val (earliest, latest) = PromptComService.fromNowToTheNext4Weeks(
        Clock.fixed(
          Instant.parse(now),
          ZoneId.of("Europe/London"),
        ),
      )
      assertThat(earliest).isEqualTo(LocalDate.parse(start))
      assertThat(latest).isEqualTo(LocalDate.parse(end))
    }
  }
}
