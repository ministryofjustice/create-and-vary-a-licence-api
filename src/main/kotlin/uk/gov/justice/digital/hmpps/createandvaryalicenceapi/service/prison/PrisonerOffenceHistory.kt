package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PrisonerOffenceHistory(
  val bookingId: Long,
  val offenceCode: String,
  val primaryResultCode: String? = null,
  val secondaryResultCode: String? = null
)
