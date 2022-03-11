package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerForRelease
import javax.validation.constraints.NotBlank

@Schema(description = "Describes a COM's contact details and the upcoming releases that they must consider for licence creation")
data class PromptLicenceCreationRequest(
  @Schema(description = "The email address of the COM", example = "jbloggs@probation.gov.uk")
  @field:NotBlank
  val email: String,

  @Schema(description = "The full name of the COM", example = "Joseph Bloggs")
  @field:NotBlank
  val comName: String,

  @Schema(description = "The list of prisoners for whom the COM should be notified of needing a licence")
  val initialPromptCases: List<PrisonerForRelease> = emptyList(),

  @Schema(description = "The list of prisoners for whom the COM should be notified of needing a licence urgently")
  val urgentPromptCases: List<PrisonerForRelease> = emptyList(),
)
