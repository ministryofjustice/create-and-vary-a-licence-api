package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Response representing a case with a licence status coming up for release")
data class LicenceStatusResponse(
  @field:Schema(description = "Name of the probation region handling the case", example = "Test Region", nullable = true)
  val probationRegion: String?,

  @field:Schema(description = "The prison for the case", example = "Moorland (HMP & YOI)", nullable = true)
  val prison: String?,

  @field:Schema(description = "Case reference number (CRN) from probation system", example = "X123456", nullable = true)
  val crn: String?,

  @field:Schema(description = "The prison nomis number for the offender", example = "A1234AA", nullable = true)
  val nomisNumber: String?,

  @field:Schema(description = "Full name of the prisoner", example = "Test Person")
  val prisonerName: String,

  @field:Schema(description = "Current licence status of the case", example = "IN_PROGRESS")
  val status: LicenceStatus,
)
