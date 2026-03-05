package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC

data class HdcStatuses(
  private val hdcStatuses: Map<Long, HdcStatus>,
) : Map<Long, HdcStatus> by hdcStatuses {

  constructor(list: List<HdcStatusHolder>) : this(
    list.associate { holder: HdcStatusHolder ->
      holder.bookingId as Long to holder.hdcStatus
    },
  )

  fun isWaitingForActivation(kind: LicenceKind, bookingId: Long): Boolean {
    if (kind != HDC) return false
    return when (hdcStatuses[bookingId]) {
      null -> true
      HdcStatus.APPROVED, HdcStatus.NOT_A_HDC_RELEASE -> false
      else -> true
    }
  }

  fun canBeActivated(kind: LicenceKind, bookingId: Long): Boolean {
    val approved = hdcStatuses[bookingId] == HdcStatus.APPROVED
    return (kind == HDC && approved) ||
      (kind != HDC && !approved)
  }

  fun isExpectedHdcRelease(bookingId: Long): Boolean = hdcStatuses[bookingId]?.let { it != HdcStatus.NOT_A_HDC_RELEASE } == true
}
