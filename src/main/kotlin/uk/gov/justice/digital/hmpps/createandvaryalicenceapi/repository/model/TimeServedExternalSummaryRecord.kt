package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase

data class TimeServedExternalSummaryRecord(
  val bookingId: Long,
  val lastWorkedOnFirstName: String?,
  val lastWorkedOnLastName: String?,
) {
  val lastWorkedOnBy: String
    get() = listOfNotNull(lastWorkedOnFirstName, lastWorkedOnLastName)
      .joinToString(" ")
      .convertToTitleCase()
      .trim()
}
