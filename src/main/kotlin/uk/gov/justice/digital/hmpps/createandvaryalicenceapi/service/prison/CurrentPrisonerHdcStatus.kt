package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class CurrentPrisonerHdcStatus(
  override val bookingId: Long? = null,
  val currentHdcStatus: HdcStatus = HdcStatus.NOT_A_HDC_RELEASE,
) : HdcStatusHolder {
  override fun isHdcRelease() = currentHdcStatus != HdcStatus.NOT_A_HDC_RELEASE
  override fun isApproved() = currentHdcStatus == HdcStatus.APPROVED
}
