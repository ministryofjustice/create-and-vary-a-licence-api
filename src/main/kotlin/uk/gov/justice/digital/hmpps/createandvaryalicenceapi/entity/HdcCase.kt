package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress
import java.beans.Transient

interface HdcCase {
  val bookingId: Long?
  var weeklyCurfewTimes: MutableList<CurfewTimes>
  var firstNightCurfewTimes: CurfewTimes?
  val curfewAddress: HdcCurfewAddress?

  @Transient
  fun isCurfewSameTimeEachDay() = weeklyCurfewTimes.all {
    it.fromTime == weeklyCurfewTimes[0].fromTime &&
      it.untilTime == weeklyCurfewTimes[0].untilTime
  }
}
