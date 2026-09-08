package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.migration.noRetryExceptions

class OffenderManagerNotFoundException : Exception("Could not find offender manager in delius")
