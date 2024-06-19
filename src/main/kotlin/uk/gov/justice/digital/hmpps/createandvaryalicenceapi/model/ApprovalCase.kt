package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes an approval case")
data class ApprovalCase(

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val licenceId: Long? = null,

  @Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String? = null,

  @Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonerNumber: String? = null,

  @Schema(description = "The full name of the person who last submitted this licence", example = "Jane Doe")
  val submittedByFullName: String? = null,

  @Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @Schema(description = "Whether an urgent approval is needed for this person", example = "false")
  val urgentApproval: Boolean? = null,

  @Schema(description = "The username who approved the licence on behalf of the prison governor", example = "X33221")
  val approvedBy: String? = null,

  @Schema(description = "The date and time that this prison approved this licence", example = "19/06/2024 09:00:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedOn: LocalDateTime? = null,

  @Schema(description = "Is the prisoner due for early release", example = "false")
  val isDueForEarlyRelease: Boolean? = null,

  @Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner? = null,
)
