package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Response representing a case with Electronic monitoring conditions, coming up for release")
data class UpcomingReleasesWithMonitoringConditionsResponse(
  @field:Schema(description = "Unique prison number of the prisoner", example = "A1234BC")
  val prisonNumber: String,
  @field:Schema(description = "Case reference number (CRN) from probation system", example = "X123456")
  val crn: String,
  @field:Schema(description = "Current licence status of the case", example = "IN_PROGRESS")
  val status: LicenceStatus,
)
