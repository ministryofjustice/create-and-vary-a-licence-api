package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import com.fasterxml.jackson.annotation.JsonProperty

data class CurrentPrisonerHdcStatus(
  override val bookingId: Long,

  @JsonProperty("status")
  override val hdcStatus: HdcStatus,
) : HdcStatusHolder {
  override fun isHdcRelease() = hdcStatus != HdcStatus.NOT_A_HDC_RELEASE
  override fun isApproved() = hdcStatus == HdcStatus.APPROVED
}
