package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

interface HdcStatusHolder {
  val bookingId: Long?
  fun isApproved(): Boolean
  fun isHdcRelease(): Boolean
}
