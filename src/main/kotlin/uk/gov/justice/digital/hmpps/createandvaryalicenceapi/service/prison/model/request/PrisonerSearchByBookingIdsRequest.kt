package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request

data class PrisonerSearchByBookingIdsRequest(
  val bookingIds: List<Long>,
)
