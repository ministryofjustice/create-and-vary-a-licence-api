package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress
import java.beans.Transient
import java.time.LocalDate

interface HdcCase {
  val bookingId: Long?
  var weeklyCurfewTimes: MutableList<CurfewTimes>
  var firstNightCurfewTimes: CurfewTimes?
  val curfewAddress: HdcCurfewAddress?
  var updatedByUsername: String?
  var updatedBy: Staff?
  var homeDetentionCurfewEndDate: LocalDate?

  @Transient
  fun isCurfewSameTimeEachDay() = weeklyCurfewTimes.all {
    it.fromTime == weeklyCurfewTimes[0].fromTime &&
      it.untilTime == weeklyCurfewTimes[0].untilTime
  }

  fun updateWeeklyCurfewTimes(
    updatedWeeklyCurfewTimes: List<CurfewTimes>,
    staffMember: Staff?,
  ) {
    weeklyCurfewTimes.apply {
      clear()
      addAll(updatedWeeklyCurfewTimes)
    }
    updatedByUsername = staffMember?.username ?: SYSTEM_USER
    updatedBy = staffMember ?: updatedBy
  }
}
