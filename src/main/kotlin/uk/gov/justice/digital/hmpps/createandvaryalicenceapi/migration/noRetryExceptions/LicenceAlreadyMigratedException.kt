package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class LicenceAlreadyMigratedException : Exception {

  constructor(licenceId: Long) :
    super("Licence $licenceId has already been migrated")

  constructor(message: String) :
    super(message)
}
