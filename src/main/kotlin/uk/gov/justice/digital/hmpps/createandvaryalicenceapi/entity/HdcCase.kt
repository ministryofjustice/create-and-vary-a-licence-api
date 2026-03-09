package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

interface HdcCase {
  val bookingId: Long?
  var hdcWeeklyCurfewTimes: MutableList<CurfewTimes>
  val curfewAddress: HdcCurfewAddress?
}
