package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PrisonerHdcStatus(
  val approvalStatus: String?,
  val approvalStatusDate: String?,
  val bookingId: Int?,
  val checksPassedDate: String?,
  val passed: Boolean,
  val refusedReason: String?
)
