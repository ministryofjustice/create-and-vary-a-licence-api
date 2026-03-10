package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ISRPssProgressionServiceTest {

  private val chunkService: ISRPssProgressionChunkService = mock()
  private val repository: ISRProgressionLicenceRepository = mock()
  private val fixedClock = Clock.fixed(CLOCK_TIME, ZoneId.systemDefault())
  private val service = ISRPssProgressionService(
    chunkService,
    repository,
    CUTOFF_DATE,
    fixedClock,
  )

  private fun rangeIds(start: Long, end: Long) = (start..end).toList()

  @Test
  fun `should process active PSS and AP_PSS licences`() {
    // Given
    val pssIds = listOf(1L, 2L)
    val apPssIds = listOf(3L, 4L)

    whenever(repository.findActiveLicenceIds(LicenceType.PSS.toString()))
      .thenReturn(pssIds)

    whenever(repository.findActiveLicenceIds(LicenceType.AP_PSS.toString()))
      .thenReturn(apPssIds)

    // When
    service.processActiveApPssAndPssLicences()

    // Then
    verify(chunkService).processActivePssLicenceChunk(pssIds)
    verify(chunkService).processActiveApPssLicenceChunk(apPssIds)
  }

  @Test
  fun `should process in flight AP_PSS licences before cutoff`() {
    // Given
    val ids = listOf(10L, 20L)

    whenever(
      repository.findInFlightLicenceIds(CUTOFF_DATE, LicenceType.AP_PSS.toString()),
    ).thenReturn(ids)

    // When
    service.processInFlightApPssLicences()

    // Then
    verify(chunkService).processApPssInFlightLicenceChunk(ids)
  }

  @Test
  fun `should not process in flight licences after cutoff`() {
    // Given
    val lateClock = Clock.fixed(
      LATE_CLOCK_TIME,
      ZoneId.systemDefault(),
    )

    val lateService = ISRPssProgressionService(
      chunkService,
      repository,
      CUTOFF_DATE,
      lateClock,
    )

    // When
    lateService.processInFlightApPssLicences()

    // Then
    verifyNoInteractions(repository)
    verifyNoInteractions(chunkService)
  }

  @Test
  fun `should split in flight licences into batches of 100`() {
    // Given
    val ids = rangeIds(1L, 250L)

    whenever(
      repository.findInFlightLicenceIds(CUTOFF_DATE, LicenceType.AP_PSS.toString()),
    ).thenReturn(ids)

    val captor = argumentCaptor<List<Long>>()

    // When
    service.processInFlightApPssLicences()

    // Then
    verify(chunkService, times(3)).processApPssInFlightLicenceChunk(captor.capture())

    assertThat(captor.allValues.map { it.size }).containsExactly(100, 100, 50)
    assertThat(captor.allValues.flatten()).containsExactlyElementsOf(ids)
  }

  @Test
  fun `should split active PSS and AP_PSS licences into batches of 100`() {
    // Given
    val pssIds = rangeIds(1L, 250L)
    val apPssIds = rangeIds(300L, 420L)

    whenever(repository.findActiveLicenceIds(LicenceType.PSS.toString()))
      .thenReturn(pssIds)

    whenever(repository.findActiveLicenceIds(LicenceType.AP_PSS.toString()))
      .thenReturn(apPssIds)

    val pssCaptor = argumentCaptor<List<Long>>()
    val apCaptor = argumentCaptor<List<Long>>()

    // When
    service.processActiveApPssAndPssLicences()

    // Then
    verify(chunkService, atLeastOnce()).processActivePssLicenceChunk(pssCaptor.capture())
    verify(chunkService, atLeastOnce()).processActiveApPssLicenceChunk(apCaptor.capture())

    assertThat(pssCaptor.allValues.map { it.size }).containsExactly(100, 100, 50)
    assertThat(apCaptor.allValues.map { it.size }).containsExactly(100, 21)

    assertThat(pssCaptor.allValues.flatten()).containsExactlyElementsOf(pssIds)
    assertThat(apCaptor.allValues.flatten()).containsExactlyElementsOf(apPssIds)
  }

  @Test
  fun `should return false when repeal date has not passed`() {
    // When
    val result = service.isRepealDatePassed()

    // Then
    assertThat(result).isFalse()
  }

  @Test
  fun `should return true when repeal date has passed`() {
    // Given
    val lateClock = Clock.fixed(
      LATE_CLOCK_TIME,
      ZoneId.systemDefault(),
    )

    val lateService = ISRPssProgressionService(
      chunkService,
      repository,
      CUTOFF_DATE,
      lateClock,
    )

    // When
    val result = lateService.isRepealDatePassed()

    // Then
    assertThat(result).isTrue()
  }

  private companion object {
    val CUTOFF_DATE: LocalDate = LocalDate.of(2026, 4, 30)
    val CLOCK_TIME: Instant = Instant.parse("2026-04-01T10:00:00Z")
    val LATE_CLOCK_TIME: Instant = Instant.parse("2026-05-01T05:00:00Z")
  }
}
