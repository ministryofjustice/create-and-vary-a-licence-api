package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PrisonerHdcStatus(
  val approvalStatus: String? = null,
  val approvalStatusDate: String? = null,
  override val bookingId: Long? = null,
  val checksPassedDate: String? = null,
  val passed: Boolean,
  val refusedReason: String? = null,
) : HdcStatusHolder {
  override fun isApproved() = approvalStatus == "APPROVED"
  override fun isHdcRelease() = isApproved()
}
