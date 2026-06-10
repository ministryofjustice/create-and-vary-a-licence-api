package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress

interface HdcCase {
  val bookingId: Long?
  var weeklyCurfewTimes: MutableList<CurfewTimes>
  var firstNightCurfewTimes: CurfewTimes?
  val curfewAddress: HdcCurfewAddress?
}
