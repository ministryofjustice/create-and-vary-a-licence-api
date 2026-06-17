package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER

interface HdcCurfewUpdatable {

  var weeklyCurfewTimes: MutableList<CurfewTimes>
  var updatedByUsername: String?
  var updatedBy: Staff?

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
