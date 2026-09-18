package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class ExistingCvlLicenceException : Exception("Licence for prisoner already exists in CVL")
