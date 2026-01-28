package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Schema(description = "Response representing a case with Electronic monitoring conditions, coming up for release")
data class UpcomingReleasesWithMonitoringConditionsResponse(
  @field:Schema(description = "Unique prison number of the prisoner", example = "A1234BC")
  val prisonNumber: String,
  @field:Schema(description = "Case reference number (CRN) from probation system", example = "X123456")
  val crn: String,
  @field:Schema(description = "Current licence status of the case", example = "IN_PROGRESS")
  val status: LicenceStatus,
  @field:Schema(description = "Licence start date", example = "15/07/2024")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate?,
  @field:Schema(description = "em condition codes", example = "14a, 14b, 14c, 5a")
  val emConditionCodes: String?,
  @field:Schema(description = "full name of prisoner", example = "Forename Surname")
  val fullName: String?,
)
