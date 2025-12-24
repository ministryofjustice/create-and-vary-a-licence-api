package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model

data class UpcomingReleasesWithMonitoringConditions(
  val prisonNumber: String,
  val crn: String,
  val status: String,
)
