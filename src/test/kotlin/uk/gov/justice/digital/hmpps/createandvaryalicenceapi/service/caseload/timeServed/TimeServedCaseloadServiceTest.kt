package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId.systemDefault

class TimeServedCaseloadServiceTest {

  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val releaseDateService: ReleaseDateService = mock()
  private val fixedClock: Clock = Clock.fixed(
    Instant.parse("2025-09-23T15:00:00Z"),
    systemDefault(),
  )
  private val service = TimeServedCaseloadService(prisonerSearchApiClient, releaseDateService, fixedClock)

  @BeforeEach
  fun setup() {
    reset(prisonerSearchApiClient)
  }

  @Test
  fun `should classify prisoner as CRDS time served`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today,
      confirmedReleaseDate = today,
      conditionalReleaseDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))

    val result = service.getCases("ABC")

    assertThat(result.identifiedCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByCrdsRule).isTrue()
        assertThat(it.isTimeServedCase).isTrue()
      }
  }

  @Test
  fun `should classify prisoner as non-CRDS time served`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today,
      confirmedReleaseDate = today,
      conditionalReleaseDate = today.minusDays(1),
      conditionalReleaseDateOverrideDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))

    val result = service.getCases("XYZ")

    assertThat(result.identifiedCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByNonCrdsRule).isTrue()
        assertThat(it.isTimeServedCase).isTrue()
      }
  }

  @Test
  fun `should classify prisoner as time served by all prison rule, prioritising override date if present`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today,
      confirmedReleaseDate = today,
      conditionalReleaseDate = today.minusDays(1),
      conditionalReleaseDateOverrideDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))

    val result = service.getCases("MDI")

    assertThat(result.identifiedCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByAllPrisonRule).isTrue()
        assertThat(it.isTimeServedCase).isTrue()
      }
  }

  @Test
  fun `should classify prisoner as time served by all prison rule, when conditional override date is missing`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today,
      confirmedReleaseDate = today,
      conditionalReleaseDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))

    val result = service.getCases("MDI")

    assertThat(result.identifiedCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByAllPrisonRule).isTrue()
        assertThat(it.isTimeServedCase).isTrue()
      }
  }

  @Test
  fun `should classify prisoner as non time served if rules do not match`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today.minusDays(1),
      confirmedReleaseDate = today,
      conditionalReleaseDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))

    val result = service.getCases("MDI")

    assertThat(result.otherCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByCrdsRule).isFalse()
        assertThat(it.isTimeServedCaseByNonCrdsRule).isFalse()
        assertThat(it.isTimeServedCaseByAllPrisonRule).isFalse()
        assertThat(it.isTimeServedCase).isFalse()
      }
  }

  @Test
  fun `should classify prisoner as time served by ignoring ARD rule when sentence start matches CRD`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today,
      confirmedReleaseDate = today.plusDays(1),
      conditionalReleaseDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))
    whenever(releaseDateService.isTimeServed(prisoner)).thenReturn(true)

    val result = service.getCases("MDI")

    assertThat(result.identifiedCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByIgnoreArdRule).isTrue()
        assertThat(it.isTimeServedCase).isTrue()
      }
  }

  @Test
  fun `should classify prisoner as time served by ignoring ARD rule when sentence start matches CRD override`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today,
      conditionalReleaseDate = today,
      conditionalReleaseDateOverrideDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))
    whenever(releaseDateService.isTimeServed(prisoner)).thenReturn(true)

    val result = service.getCases("MDI")

    assertThat(result.identifiedCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByIgnoreArdRule).isTrue()
        assertThat(it.isTimeServedCase).isTrue()
      }
  }

  @Test
  fun `should not classify prisoner as time served by ignoring ARD rule when sentence start does not match CRD`() {
    val today = LocalDate.now(fixedClock)
    val prisoner = prisonerSearchResult(
      sentenceStartDate = today.minusDays(1),
      confirmedReleaseDate = today,
      conditionalReleaseDate = today,
    )

    whenever(prisonerSearchApiClient.searchPrisonersByReleaseDate(any(), any(), any(), anyOrNull()))
      .thenReturn(PageImpl(listOf(prisoner)))

    val result = service.getCases("MDI")

    assertThat(result.otherCases)
      .hasSize(1)
      .allSatisfy {
        assertThat(it.isTimeServedCaseByIgnoreArdRule).isFalse()
        assertThat(it.isTimeServedCase).isFalse()
      }
  }
}
