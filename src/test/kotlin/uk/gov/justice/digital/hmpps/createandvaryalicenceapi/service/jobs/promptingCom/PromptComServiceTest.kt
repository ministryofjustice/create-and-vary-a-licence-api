package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PromptComServiceTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val deliusApiClient = mock<DeliusApiClient>()

  private val promptComService =
    PromptComService(
      licenceRepository,
      prisonerSearchApiClient,
      probationSearchApiClient,
      eligibilityService,
      releaseDateService,
      prisonApiClient,
      deliusApiClient,
    )

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
      val (earliest, latest) = promptComService.fromNowToTheNext4Weeks(
        Clock.fixed(
          Instant.parse(now),
          ZoneId.systemDefault(),
        ),
      )
      assertThat(earliest).isEqualTo(LocalDate.parse(start))
      assertThat(latest).isEqualTo(LocalDate.parse(end))
    }
  }
}
