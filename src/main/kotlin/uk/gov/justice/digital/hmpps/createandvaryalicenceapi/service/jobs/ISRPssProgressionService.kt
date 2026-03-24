package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.LocalDate
import kotlin.collections.chunked

@Service
class ISRPssProgressionService(
  private val chunkService: ISRPssProgressionChunkService,
  private val repository: ISRProgressionLicenceRepository,
  @param:Value("\${feature.toggle.isr.repeal.date:#{null}}")
  private val isrRepealDate: LocalDate?,
  private val clock: Clock = Clock.systemDefaultZone(),
) {

  fun process() {
    if (isrRepealDate == null) {
      log.info("ISR progression skipped because repeal date is not configured")
      return
    }
    if (getCurrentDate().isBefore(isrRepealDate)) {
      log.info("ISR progression job skipped because appeal {} date has not been reached", getCurrentDate())
      return
    }

    processPssLicences()
    processApPssLicences()
  }

  private fun processPssLicences() {
    val pssLicenceIds = repository.findInFlightAndActiveLicenceIds(LicenceType.PSS.toString())

    log.info("ISR PSS progression found {} to process", pssLicenceIds.size)
    // Cut the pssLicenceIds into batches/chunks to allow smaller transaction sizes
    pssLicenceIds.chunked(BATCH_SIZE).forEach {
      chunkService.processPssLicenceChunk(it)
    }
  }

  private fun processApPssLicences() {
    val apPssLicenceIds = repository.findInFlightAndActiveLicenceIds(LicenceType.AP_PSS.toString())

    log.info("ISR AP_PSS repealed licence, found {} to process", apPssLicenceIds.size)
    // Cut the apPssLicenceIds into batches/chunks to allow smaller transaction sizes
    apPssLicenceIds.chunked(BATCH_SIZE).forEach {
      chunkService.processApPssLicenceChunk(it)
    }
  }

  fun isRepealDatePassed(): Boolean = isrRepealDate?.let { !getCurrentDate().isBefore(it) } ?: false

  private fun getCurrentDate(): LocalDate = LocalDate.now(clock)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val BATCH_SIZE = 100
  }
}
