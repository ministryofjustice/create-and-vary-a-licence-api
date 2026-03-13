package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

interface HdcCase {
  val bookingId: Long?
  var weeklyCurfewTimes: MutableList<CurfewTimes>
  var firstNightCurfewTimes: CurfewTimes?
  val curfewAddress: HdcCurfewAddress?
}
