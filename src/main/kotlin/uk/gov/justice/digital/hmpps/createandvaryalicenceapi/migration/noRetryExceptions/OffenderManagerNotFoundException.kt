package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class OffenderManagerNotFoundException (prisonNumber: String) : Exception("Could not find offender manager for $prisonNumber in delius")