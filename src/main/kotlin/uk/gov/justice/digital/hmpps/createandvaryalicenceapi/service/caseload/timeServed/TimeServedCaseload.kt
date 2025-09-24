package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a Time Served case")
data class TimeServedCaseload(
  @field:Schema(description = "List of the cases we have identified as being time served cases")
  val identifiedCases: List<TimeServedCase>,

  @field:Schema(description = "Other cases coming up for release")
  val otherCases: List<TimeServedCase>,
)
