package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class ExistingCvlLicenceException(prisonNumber: String) : Exception("Licence for prisoner $prisonNumber already exists")
