package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatusHolder

data class PrisonerHdcStatus(
  val approvalStatus: String? = null,
  val approvalStatusDate: String? = null,
  override val bookingId: Long? = null,
  val checksPassedDate: String? = null,
  val passed: Boolean,
  val refusedReason: String? = null,
) : HdcStatusHolder {
  override val currentHdcStatus: HdcStatus = if(isApproved()) HdcStatus.APPROVED else HdcStatus.NOT_A_HDC_RELEASE
  override fun isApproved() = approvalStatus == "APPROVED"
  override fun isHdcRelease() = isApproved()
}
