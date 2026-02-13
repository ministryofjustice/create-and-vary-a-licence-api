package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Describes a com search result which has been found and enriched")
data class FoundComCase(
  @field:Schema(description = "kind of licence", example = "CRD")
  val kind: LicenceKind,

  @field:Schema(description = "the prison booking id", example = "123")
  val bookingId: Long? = null,

  @field:Schema(description = "The forename and surname of the offender")
  val name: String = "",

  @field:Schema(
    description = "The case reference number (CRN) of the offender,",
    example = "X12344",
  )
  val crn: String? = "",

  @field:Schema(description = "The prison nomis number for the offender", example = "A1234AA")
  val nomisId: String? = "",

  @Deprecated("Use probationPractitioner name instead")
  @field:Schema(description = "The forename and surname of the COM")
  val comName: String? = "",

  @Deprecated("Use probationPractitioner staffCode instead")
  @field:Schema(description = "The COM's staff code")
  val comStaffCode: String? = "",

  @field:Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner,

  @field:Schema(description = "The description of the COM's team")
  val teamName: String? = "",

  @field:Schema(description = "The release date of the offender", example = "27/07/2023")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @field:Schema(
    description = "The date when the hard stop period starts",
    example = "11/09/2022",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @field:Schema(
    description = "The date when warning about the hard stop period begins",
    example = "11/09/2022",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @field:Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,

  @field:Schema(description = "Is the prisoner due to be released in the next two working days")
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean = false,

  @field:Schema(description = "The ID of the most recent and relevant licence", example = "123344")
  val licenceId: Long? = null,

  @field:Schema(description = "The licence Id which this licence is a version of", example = "86")
  val versionOf: Long? = null,

  @field:Schema(description = "The type of licence")
  val licenceType: LicenceType? = null,

  @field:Schema(description = "The status of the licence")
  val licenceStatus: LicenceStatus? = null,

  @field:Schema(description = "Indicates whether the offender is in prison or out on probation")
  val isOnProbation: Boolean? = null,

  @field:Schema(description = "Label for release date", example = "Confirmed release date")
  val releaseDateLabel: String? = null,

  @field:Schema(description = "Is a review of this licence is required", example = "true")
  val isReviewNeeded: Boolean? = null,

  @field:Schema(description = "Is the offender a limited access offender (LAO)?", example = "true")
  val isLao: Boolean? = null,
) {
  companion object {
    fun restrictedCase(kind: LicenceKind, crn: String?, isOnProbation: Boolean): FoundComCase = FoundComCase(
      kind = kind,
      name = "Access restricted on NDelius",
      crn = crn,
      comName = "Restricted",
      probationPractitioner = ProbationPractitioner.laoProbationPractitioner(),
      isOnProbation = isOnProbation,
      isLao = true,
    )
  }
}
