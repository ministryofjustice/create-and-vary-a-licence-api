package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class PrisonerReleasedOnExistingCvlLicenceException : Exception("HDC Licence should not be used, the prisoner has already been release on a CVL Licence!")
