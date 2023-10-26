package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class CourtEventOutcome(
  val bookingId: Long,
  val eventId: Long,
  val outcomeReasonCode: String?,
)
