package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.request.MigrateFromHdcToCvlRequest

@Service
class MigrationService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun migrate(request: MigrateFromHdcToCvlRequest) {
    log.info("Starting migration for bookingId={}", request.bookingId)
    // TODO - implement migration logic here
    log.info("Ending migration for bookingId={}", request.bookingId)
  }
}
