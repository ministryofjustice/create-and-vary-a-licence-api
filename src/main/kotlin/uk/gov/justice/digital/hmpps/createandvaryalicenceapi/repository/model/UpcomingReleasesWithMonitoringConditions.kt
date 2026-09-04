package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import java.time.LocalDate
import java.time.LocalDateTime

data class UpcomingReleasesWithMonitoringConditions(
  val prisonNumber: String,
  val crn: String,
  val status: String,
  val licenceStartDate: LocalDate?,
  val submittedDate: LocalDateTime?,
  val emConditionCodes: String?,
  val forename: String?,
  val surname: String?,
) {

  val fullName: String
    get() = listOfNotNull(forename, surname).joinToString(" ").convertToTitleCase().trim()
}
