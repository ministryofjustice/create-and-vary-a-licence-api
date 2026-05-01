package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class LicenceAlreadyMigratedException(licenceId: Long) : Exception("Licence $licenceId has already been migrated")
