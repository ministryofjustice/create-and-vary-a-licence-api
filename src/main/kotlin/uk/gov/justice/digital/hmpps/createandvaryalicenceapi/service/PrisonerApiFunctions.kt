package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerOffenceHistory

object IS91Constants {
  val resultCodes = setOf("5500", "4022", "3006", "5502")
  const val offenceCode = "IA99000-001N"
}

fun getIS91AndExtraditionBookingIds(offenceHistories: List<PrisonerOffenceHistory>): List<Long> {
  val is91AndExtraditionOffenceHistories = offenceHistories.filter {
    IS91Constants.resultCodes.contains(it.primaryResultCode) ||
      IS91Constants.resultCodes.contains(it.secondaryResultCode) ||
      it.offenceCode == IS91Constants.offenceCode
  }
  return is91AndExtraditionOffenceHistories.map { it.bookingId }
}
