package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Schema(description = "Response representing a last-minute handover case")
data class LastMinuteHandoverCaseResponse(

  @Schema(description = "Planned release date of the prisoner", example = "2025-10-15")
  val releaseDate: LocalDate,

  @Schema(description = "Name of the probation region handling the case", nullable = true)
  val probationRegion: String?,

  @Schema(description = "Unique prison number of the prisoner", example = "A1234BC")
  val prisonerNumber: String,

  @Schema(description = "Case reference number (CRN) from probation system", example = "X123456", nullable = true)
  val crn: String?,

  @Schema(description = "Full name of the prisoner", example = "John Smith")
  val prisonerName: String,

  @Schema(description = "Assigned probation practitioner", nullable = true)
  val probationPractitioner: String?,

  @Schema(description = "Current licence status of the case", example = "IN_PROGRESS")
  val status: LicenceStatus,

  @Schema(description = "Prison code where the prisoner is held", example = "LEI", nullable = true)
  val prisonCode: String?,
)
