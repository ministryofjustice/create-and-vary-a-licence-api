package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

interface HdcCase {
  val bookingId: Long?
  var curfewTimes: MutableList<HdcCurfewTimes>
  val curfewAddress: HdcCurfewAddress?
}
