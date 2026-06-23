package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.ISRProgressionLicenceRepository

@Service
class MigrateStandardConditionsService(
  private val isrProgressionLicenceRepository: ISRProgressionLicenceRepository,
  private val migrateStandardConditionsChunkService: MigrateStandardConditionsChunkService,
) {

  @Async
  fun migrateStandardConditions(policyVersion: String) {
    log.info("Migrating standard conditions on in flight licences to version $policyVersion")
    val inflightLicenceIds =
      isrProgressionLicenceRepository.findInFlightLicenceIds()
    log.info("Found ${inflightLicenceIds.size} inflight licences to potentially update")
    inflightLicenceIds.chunked(BATCH_SIZE).forEach {
      migrateStandardConditionsChunkService.migrateStandardConditions(it, policyVersion)
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val BATCH_SIZE = 100
  }
}
