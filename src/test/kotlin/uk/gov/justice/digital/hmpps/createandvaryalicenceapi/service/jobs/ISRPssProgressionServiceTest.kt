package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ISRPssProgressionServiceTest {

  private val chunkService: ISRPssProgressionChunkService = mock()
  private val repository: ISRProgressionLicenceRepository = mock()
  private val fixedClock = Clock.fixed(CLOCK_TIME, ZoneId.systemDefault())

  @Test
  fun `should process both PSS and AP_PSS licences when repeal date reached`() {
    // Given
    val service = buildService(REPEAL_DATE)

    val pssIds = listOf(1L, 2L)
    val apPssIds = listOf(3L, 4L)

    whenever(repository.findInFlightAndActiveLicenceIds("PSS"))
      .thenReturn(pssIds)

    whenever(repository.findInFlightAndActiveLicenceIds("AP_PSS"))
      .thenReturn(apPssIds)

    // When
    service.process()

    // Then
    verify(chunkService).processPssLicenceChunk(pssIds)
    verify(chunkService).processApPssLicenceChunk(apPssIds)
  }

  @Test
  fun `should process when time is exactly midnight on repeal date`() {
    // Given
    val midnightClock = Clock.fixed(
      Instant.parse("2026-05-11T00:00:00Z"),
      ZoneId.systemDefault(),
    )

    val service = ISRPssProgressionService(
      chunkService,
      repository,
      REPEAL_DATE,
      midnightClock,
    )

    val pssIds = listOf(1L)
    val apPssIds = listOf(2L)

    whenever(repository.findInFlightAndActiveLicenceIds("PSS"))
      .thenReturn(pssIds)

    whenever(repository.findInFlightAndActiveLicenceIds("AP_PSS"))
      .thenReturn(apPssIds)

    // When
    service.process()

    // Then
    verify(chunkService).processPssLicenceChunk(pssIds)
    verify(chunkService).processApPssLicenceChunk(apPssIds)
  }

  @Test
  fun `should not process when before repeal date`() {
    // Given
    val earlyClock = Clock.fixed(EARLY_CLOCK_TIME, ZoneId.systemDefault())

    val service = ISRPssProgressionService(
      chunkService,
      repository,
      REPEAL_DATE,
      earlyClock,
    )

    // When
    service.process()

    // Then
    verifyNoInteractions(repository)
    verifyNoInteractions(chunkService)
  }

  @Test
  fun `should not process when repeal date is null`() {
    // Given
    val service = buildService(null)

    // When
    service.process()

    // Then
    verifyNoInteractions(repository)
    verifyNoInteractions(chunkService)
  }

  @Test
  fun `should split PSS licences into batches of 100`() {
    // Given
    val service = buildService(REPEAL_DATE)

    val ids = rangeIds(1L, 250L)

    whenever(repository.findInFlightAndActiveLicenceIds("PSS"))
      .thenReturn(ids)

    whenever(repository.findInFlightAndActiveLicenceIds("AP_PSS"))
      .thenReturn(emptyList())

    val captor = argumentCaptor<List<Long>>()

    // When
    service.process()

    // Then
    verify(chunkService, times(3)).processPssLicenceChunk(captor.capture())

    assertThat(captor.allValues.map { it.size }).containsExactly(100, 100, 50)
    assertThat(captor.allValues.flatten()).containsExactlyElementsOf(ids)
  }

  @Test
  fun `should split AP_PSS licences into batches of 100`() {
    // Given
    val service = buildService(REPEAL_DATE)

    val ids = rangeIds(1L, 220L)

    whenever(repository.findInFlightAndActiveLicenceIds("PSS"))
      .thenReturn(emptyList())

    whenever(repository.findInFlightAndActiveLicenceIds("AP_PSS"))
      .thenReturn(ids)

    val captor = argumentCaptor<List<Long>>()

    // When
    service.process()

    // Then
    verify(chunkService, times(3)).processApPssLicenceChunk(captor.capture())

    assertThat(captor.allValues.map { it.size }).containsExactly(100, 100, 20)
    assertThat(captor.allValues.flatten()).containsExactlyElementsOf(ids)
  }

  @Test
  fun `should return false before repeal date`() {
    // Given
    val earlyClock = Clock.fixed(EARLY_CLOCK_TIME, ZoneId.systemDefault())

    val service = ISRPssProgressionService(
      chunkService,
      repository,
      REPEAL_DATE,
      earlyClock,
    )

    // When
    val result = service.isRepealDatePassed()

    // Then
    assertThat(result).isFalse()
  }

  @Test
  fun `should return true on repeal date`() {
    // Given
    val service = buildService(REPEAL_DATE)

    // When
    val result = service.isRepealDatePassed()

    // Then
    assertThat(result).isTrue()
  }

  @Test
  fun `should return false when repeal date is null`() {
    // Given
    val service = buildService(null)

    // When
    val result = service.isRepealDatePassed()

    // Then
    assertThat(result).isFalse()
  }

  private fun buildService(date: LocalDate?) = ISRPssProgressionService(
    chunkService,
    repository,
    date,
    fixedClock,
  )

  private fun rangeIds(start: Long, end: Long) = (start..end).toList()

  private companion object {
    val REPEAL_DATE: LocalDate = LocalDate.of(2026, 5, 11)
    val CLOCK_TIME: Instant = Instant.parse("2026-05-11T10:00:00Z")
    val EARLY_CLOCK_TIME: Instant = Instant.parse("2026-05-10T05:00:00Z")
  }
}
