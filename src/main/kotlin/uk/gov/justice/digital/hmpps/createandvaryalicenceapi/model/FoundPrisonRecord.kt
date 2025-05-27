package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Schema(description = "Describes a search result which has been found and enriched")
data class FoundPrisonRecord(
  @Schema(description = "kind of licence, null if no licence exists", example = "CRD")
  val kind: LicenceKind? = null,

  @Schema(description = "the prison booking id", example = "123")
  val bookingId: Long? = null,

  @Schema(description = "The forename and surname of the offender")
  val name: String = "",

  @Schema(
    description = "The case reference number (CRN) of the offender,",
    example = "X12344",
  )
  val crn: String? = "",

  @Schema(description = "The prison nomis number for the offender", example = "A1234AA")
  val nomisId: String? = "",

  @Schema(description = "The forename and surname of the COM")
  val comName: String? = "",

  @Schema(description = "The COM's staff code")
  val comStaffCode: String? = "",

  @Schema(description = "The description of the COM's team")
  val teamName: String? = "",

  @Schema(description = "The release date of the offender", example = "27/07/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @Schema(
    description = "The date when the hard stop period starts",
    example = "11/09/2022",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @Schema(
    description = "The date when warning about the hard stop period begins",
    example = "11/09/2022",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @Schema(description = "Is the licence in the hard stop period? (Within two working days of release)")
  val isInHardStopPeriod: Boolean = false,

  @Schema(description = "Is the prisoner due for early release")
  val isDueForEarlyRelease: Boolean = false,

  @Schema(description = "Is the prisoner due to be released in the next two working days")
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean = false,

  @Schema(description = "The ID of the most recent and relevant licence", example = "123344")
  val licenceId: Long? = null,

  @Schema(description = "The licence Id which this licence is a version of", example = "86")
  val versionOf: Long? = null,

  @Schema(description = "The type of licence")
  val licenceType: LicenceType? = null,

  @Schema(description = "The status of the licence")
  val licenceStatus: LicenceStatus? = null,

  @Schema(description = "Label for release date", example = "Confirmed release date")
  val releaseDateLabel: String? = null,

  @Schema(description = "Is a review of this licence is required", example = "true")
  val isReviewNeeded: Boolean? = null,

  )
