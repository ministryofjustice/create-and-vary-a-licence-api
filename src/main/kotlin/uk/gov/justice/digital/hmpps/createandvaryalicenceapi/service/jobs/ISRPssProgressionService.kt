package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.chunked

@Service
class ISRPssProgressionService(
  private val chunkService: ISRPssProgressionChunkService,
  private val repository: ISRProgressionLicenceRepository,
  private val clock: Clock = Clock.systemDefaultZone(),
) {

  fun processActiveApPssAndPssLicences() {
    // There will be around 530 licences in prod of PSS type
    val activePSSLicences = repository.findActiveLicenceIds(LicenceType.PSS.toString())

    log.info("ISR PSS progression found {} PSS Active licences to process", activePSSLicences.size)
    // Cut the apPssLicenceIds into batches/chunks to allow smaller transaction sizes
    activePSSLicences.chunked(BATCH_SIZE).forEach {
      processActivePssLicenceChunkSafely(it)
    }

    // There will be around 15177 licenses in prod or AP PSS type
    val activeApPSSLicences = repository.findActiveLicenceIds(LicenceType.AP_PSS.toString())

    log.info("ISR AP_PSS progression found {} AP_PSS Active licences to process", activePSSLicences.size)

    // Cut the apPssLicenceIds into batches/chunks to allow smaller transaction sizes
    activeApPSSLicences.chunked(BATCH_SIZE).forEach {
      processActiveApPssLicenceChunkSafely(it)
    }
  }

  fun processInFlightApPssLicences() {
    if (getCurrentDateAndTime().isAfter(CUTOFF_EXECUTION_DEADLINE)) {
      log.info(
        "ISR PSS progression job skipped because cutoff execution deadline {} has passed",
        CUTOFF_EXECUTION_DEADLINE,
      )
      return
    }

    val apPssLicenceIds = repository.findInFlightLicenceIds(
      CUTOFF_DATE,
      LicenceType.AP_PSS.toString(),
    )

    log.info("ISR PSS progression found {} AP PSS licences to process", apPssLicenceIds.size)
    // Cut the apPssLicenceIds into batches/chunks to allow smaller transaction sizes
    apPssLicenceIds.chunked(BATCH_SIZE).forEach {
      processApPssInFlightLicenceChunkSafely(it)
    }
  }

  private fun getCurrentDateAndTime(): LocalDateTime = LocalDateTime.now(clock)

  @Suppress("TooGenericExceptionCaught")
  private fun processApPssInFlightLicenceChunkSafely(chunkLicenceIds: List<Long>) {
    try {
      chunkService.processApPssInFlightLicenceChunk(chunkLicenceIds)
    } catch (ex: Exception) {
      log.error("ISR AP_PSS repealed in flight licence chunk failed for chunkLicenceIds: {}", chunkLicenceIds, ex)
      throw ex
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun processActivePssLicenceChunkSafely(chunkLicenceIds: List<Long>) {
    try {
      chunkService.processActivePssLicenceChunkSafely(chunkLicenceIds)
    } catch (ex: Exception) {
      log.error("ISR PSS repealed active progression chunk failed for chunkLicenceIds: {}", chunkLicenceIds, ex)
      throw ex
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun processActiveApPssLicenceChunkSafely(chunkLicenceIds: List<Long>) {
    try {
      chunkService.processActiveApPssLicenceChunkSafely(chunkLicenceIds)
    } catch (ex: Exception) {
      log.error("ISR AP_PSS repealed Active progression chunk failed for chunkLicenceIds: {}", chunkLicenceIds, ex)
      throw ex
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(this::class.java)
    private val CUTOFF_DATE: LocalDate = LocalDate.of(2026, 4, 30)
    private val CUTOFF_EXECUTION_DEADLINE: LocalDateTime = CUTOFF_DATE.atTime(23, 59).plusHours(2)
    private const val BATCH_SIZE = 100
  }
}
