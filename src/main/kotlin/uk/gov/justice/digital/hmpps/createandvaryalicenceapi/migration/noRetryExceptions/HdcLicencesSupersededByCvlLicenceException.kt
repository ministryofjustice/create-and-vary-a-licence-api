package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class HdcLicencesSupersededByCvlLicenceException : Exception("HDC Licence is superseded by a CVL Licence with a release date")
