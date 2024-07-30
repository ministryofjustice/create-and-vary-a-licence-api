package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

enum class LicenceCreationType {
  LICENCE_CHANGES_NOT_APPROVED_IN_TIME,
  PRISON_WILL_CREATE_THIS_LICENCE,
  LICENCE_CREATED_BY_PRISON,
  LICENCE_NOT_STARTED,
  LICENCE_IN_PROGRESS,
}

@Schema(description = "Describes an COM case")
data class ComCase(
  @Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String?,

  @Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crnNumber: String?,

  @Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonerNumber: String?,

  @Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate?,

  @Schema(description = "The date on which a list of returned COM cases has been sorted", example = "30/11/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val sortDate: LocalDate? = null,

  @Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val licenceId: Long?,

  @Schema(description = "The new status for this licence", example = "APPROVED")
  val licenceStatus: LicenceStatus?,

  @Schema(description = "The licence type code", example = "AP")
  val licenceType: LicenceType?,

  @Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner?,

  @Schema(description = "Date which the hard stop period will start", example = "03/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @Schema(description = "Date which to show the hard stop warning", example = "01/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @Schema(description = "Type of this licence", example = LicenceKinds.CRD)
  val kind: LicenceKind?,

  @Schema(description = "Is the prisoner due for early release", example = "false")
  val isDueForEarlyRelease: Boolean,

  @Schema(description = "How this licence will need to be created", example = "PRISON_WILL_CREATE_THIS_LICENCE")
  val licenceCreationType: LicenceCreationType? = null,
)
