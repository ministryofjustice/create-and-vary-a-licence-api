package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class CurrentPrisonerHdcStatus(
  override val bookingId: Long,
  val currentHdcStatus: HdcStatus,
) : HdcStatusHolder {
  override fun isHdcRelease() = currentHdcStatus != HdcStatus.NOT_A_HDC_RELEASE
  override fun isApproved() = currentHdcStatus == HdcStatus.APPROVED
}
