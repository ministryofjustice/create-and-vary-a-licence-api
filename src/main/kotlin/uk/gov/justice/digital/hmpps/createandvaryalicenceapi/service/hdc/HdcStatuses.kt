package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC

data class HdcStatuses(val hdcStatuses: List<HdcStatusHolder>) {

  fun asBookingIdToStatusMap(): Map<Long, HdcStatus> = hdcStatuses.associate { it.bookingId as Long to it.currentHdcStatus }

  private val statusMap: Map<Long, HdcStatus> by lazy { asBookingIdToStatusMap() }

  /**
   * RULE:
   * Waiting for activation if:
   *   - kind == HDC
   *   - AND status NOT IN [APPROVED, NOT_A_HDC_RELEASE]
   *   - null (no entry) counts as waiting
   */
  fun isWaitingForActivation(kind: LicenceKind, bookingId: Long): Boolean {
    if (kind != HDC) return false

    val status = statusMap[bookingId]
    return when (status) {
      null -> true
      HdcStatus.APPROVED, HdcStatus.NOT_A_HDC_RELEASE -> false
      else -> true
    }
  }

  /**
   * RULE:
   * Can be activated if:
   *   - kind == HDC AND status == APPROVED
   *   - OR kind != HDC AND status != APPROVED
   *   - null (no entry) counts as not approved
   */
  fun canBeActivated(kind: LicenceKind, bookingId: Long): Boolean {
    val status = statusMap[bookingId]
    val approvedForHdc = status == HdcStatus.APPROVED

    return (kind == HDC && approvedForHdc) ||
      (kind != HDC && !approvedForHdc)
  }

  /**
   * RULE:
   * Expected HDC release if:
   *   - status != NOT_A_HDC_RELEASE
   *   - null (no entry) counts as not expected
   */
  fun isExpectedHdcRelease(bookingId: Long): Boolean = statusMap[bookingId] != null && statusMap[bookingId] != HdcStatus.NOT_A_HDC_RELEASE
}
