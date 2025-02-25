package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PrisonerHdcStatus(
  val approvalStatus: String? = null,
  val approvalStatusDate: String? = null,
  val bookingId: Long? = null,
  val checksPassedDate: String? = null,
  val passed: Boolean,
  val refusedReason: String? = null,
) {
  fun isApproved() = approvalStatus == "APPROVED"
  fun isNotApproved() = !isApproved()
}
