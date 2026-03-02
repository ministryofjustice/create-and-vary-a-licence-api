package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

interface HdcStatusHolder {
  val bookingId: Long?
  val currentHdcStatus: HdcStatus
  fun isApproved(): Boolean
  fun isHdcRelease(): Boolean
}
