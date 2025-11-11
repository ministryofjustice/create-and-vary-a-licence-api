package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

data class TimeServedExternalRecordFlags(
  val bookingId: Long,
  val hasNomisLicence: Boolean,
)
