package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

data class TimeServedExternalRecordsFlag(
  val bookingId: Long,
  val hasNomisLicence: Boolean,
)
