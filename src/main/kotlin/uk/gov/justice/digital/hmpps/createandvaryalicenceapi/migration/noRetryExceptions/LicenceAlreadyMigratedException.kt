package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

import org.springframework.dao.DataIntegrityViolationException

class LicenceAlreadyMigratedException : Exception {

  constructor(licenceVersionId: Long) :
    super("Licence has already been migrated, HDC LicenceVersionId: $licenceVersionId")

  constructor(licenceVersionId: Long, e: DataIntegrityViolationException) :
    super("Licence has already been migrated, HDC LicenceVersionId: $licenceVersionId, ${e.message}")
}
