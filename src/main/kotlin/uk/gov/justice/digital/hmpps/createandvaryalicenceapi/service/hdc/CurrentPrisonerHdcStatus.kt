package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

data class CurrentPrisonerHdcStatus(
  override val bookingId: Long,
  override val hdcStatus: HdcStatus,
) : HdcStatusHolder {
  override fun isHdcRelease() = hdcStatus != HdcStatus.NOT_A_HDC_RELEASE
  override fun isApproved() = hdcStatus == HdcStatus.APPROVED
}
