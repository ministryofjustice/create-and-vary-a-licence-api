package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class ISRPssProgressionServiceTest {

  private val chunkService = mock<ISRPssProgressionChunkService>()
  private val repository = mock<ISRProgressionLicenceRepository>()

  val clockBeforeCutOff: Clock = Clock.fixed(
    Instant.parse("2026-03-01T00:00:00Z"),
    Clock.systemDefaultZone().zone,
  )

  val clockPassedCutOff: Clock = Clock.fixed(
    Instant.parse("2027-01-01T00:00:00Z"),
    Clock.systemDefaultZone().zone,
  )

  private fun createService(overrideClock: Clock): ISRPssProgressionService = ISRPssProgressionService(
    chunkService,
    repository,
    overrideClock,
  )

  @BeforeEach
  fun resetMocks() {
    reset(chunkService, repository)
  }

  @Test
  fun `should do nothing when no licences returned`() {
    whenever(
      repository.findLicenceIds(
        LocalDate.of(2026, 4, 30),
        LicenceType.AP_PSS.toString(),
      ),
    ).thenReturn(emptyList())

    createService(clockBeforeCutOff).processApPssLicences()

    verifyNoInteractions(chunkService)
  }

  @Test
  fun `should process licences in single chunk when below batch size`() {
    val licenceIds = listOf(1L, 2L, 3L)

    whenever(
      repository.findLicenceIds(
        LocalDate.of(2026, 4, 30),
        LicenceType.AP_PSS.toString(),
      ),
    ).thenReturn(licenceIds)

    createService(clockBeforeCutOff).processApPssLicences()

    verify(chunkService).processApPssLicenceChunk(licenceIds)
  }

  @Test
  fun `should split processing when licences exceed batch size`() {
    val licenceIds = (1L..250L).toList()

    whenever(
      repository.findLicenceIds(
        LocalDate.of(2026, 4, 30),
        LicenceType.AP_PSS.toString(),
      ),
    ).thenReturn(licenceIds)

    createService(clockBeforeCutOff).processApPssLicences()

    verify(chunkService).processApPssLicenceChunk((1L..100L).toList())
    verify(chunkService).processApPssLicenceChunk((101L..200L).toList())
    verify(chunkService).processApPssLicenceChunk((201L..250L).toList())

    verify(chunkService, times(3))
      .processApPssLicenceChunk(any())
  }

  @Test
  fun `should skip processing when cutoff deadline has passed`() {
    whenever(
      repository.findLicenceIds(any(), any()),
    ).thenReturn(listOf(1L))

    createService(clockPassedCutOff).processApPssLicences()

    verifyNoInteractions(chunkService)
  }
}
