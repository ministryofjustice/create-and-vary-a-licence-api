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
data class ComCreateCase(
  @field:Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String,

  @field:Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crnNumber: String?,

  @field:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonerNumber: String?,

  @field:Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate?,

  @field:Schema(description = "Unique identifier for this licence within the service", example = "99999")
  val licenceId: Long?,

  @field:Schema(description = "The new status for this licence", example = "APPROVED")
  val licenceStatus: LicenceStatus?,

  @field:Schema(description = "The licence type code", example = "AP")
  val licenceType: LicenceType,

  @field:Schema(description = "Is a review of this licence is required", example = "true")
  val isReviewNeeded: Boolean,

  @field:Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner,

  @field:Schema(description = "Date which the hard stop period will start", example = "03/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @field:Schema(description = "Date which to show the hard stop warning", example = "01/05/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @field:Schema(description = "Type of this licence", example = LicenceKinds.CRD)
  val kind: LicenceKind,

  @field:Schema(description = "How this licence will need to be created", example = "PRISON_WILL_CREATE_THIS_LICENCE")
  val licenceCreationType: LicenceCreationType? = null,

  @field:Schema(description = "Is the offender a limited access offender (LAO)?", example = "true")
  val isRestricted: Boolean,
)
