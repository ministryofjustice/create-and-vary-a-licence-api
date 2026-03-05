package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ISRPssProgressionService(
  private val chunkService: ISRPssProgressionChunkService,
  private val repository: ISRProgressionLicenceRepository,
  private val clock: Clock = Clock.systemUTC(),
) {

  fun processApPssLicences() {
    if (getCurrentDateAndTime().isAfter(CUTOFF_EXECUTION_DEADLINE)) {
      log.info(
        "ISR PSS progression job skipped because cutoff execution deadline {} has passed",
        CUTOFF_EXECUTION_DEADLINE,
      )
      return
    }

    val apPssLicenceIds = repository.findLicenceIds(
      CUTOFF_DATE,
      LicenceType.AP_PSS.toString(),
    )

    apPssLicenceIds.chunked(BATCH_SIZE).forEach {
      processChunkSafely(it)
    }
  }

  private fun getCurrentDateAndTime(): LocalDateTime = LocalDateTime.now(clock)

  @Suppress("TooGenericExceptionCaught")
  private fun processChunkSafely(chunkLicenceIds: List<Long>) {
    try {
      chunkService.processApPssLicenceChunk(chunkLicenceIds)
    } catch (ex: Exception) {
      log.error("ISR PSS progression chunk failed for chunk: {}", chunkLicenceIds, ex)
      throw ex
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    private val CUTOFF_DATE: LocalDate = LocalDate.of(2026, 4, 30)
    private val CUTOFF_EXECUTION_DEADLINE: LocalDateTime = CUTOFF_DATE.atTime(23, 59).plusMinutes(5)
    private const val BATCH_SIZE = 200
  }
}
