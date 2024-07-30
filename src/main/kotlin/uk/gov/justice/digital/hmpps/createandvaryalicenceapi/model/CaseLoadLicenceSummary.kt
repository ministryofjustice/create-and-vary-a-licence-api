package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A summary of a licence for use in the COM case load views")
data class CaseLoadLicenceSummary(
  @Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long? = null,

  @Schema(description = "The status of this licence", example = "IN_PROGRESS")
  val licenceStatus: LicenceStatus,

  @Schema(description = "Kind of licence", example = "CRD")
  val kind: LicenceKind? = null,

  @Schema(description = "Licence type code", example = "AP")
  val licenceType: LicenceType,

  @Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crn: String?,

  @Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  val nomisId: String?,

  @Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String?,

  @Schema(description = "The username of the responsible probation officer", example = "jsmith")
  val comUsername: String? = null,

  @Schema(description = "The date the licence was created", example = "02/12/2001 10:15")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val dateCreated: LocalDateTime? = null,

  @Schema(
    description = "The full name of the person who approved the licence",
    example = "John Smith",
  )
  val approvedBy: String? = null,

  @Schema(description = "The date and time that this licence was approved", example = "24/08/2022 11:30:33")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val approvedDate: LocalDateTime? = null,

  @Schema(description = "The licence Id which this licence is a version of", example = "86")
  val versionOf: Long? = null,

  @Schema(description = "The username of the person who last updated this licence", example = "John Doe")
  val updatedByFullName: String? = null,

  @Schema(description = "Date which to show the hard stop warning", example = "01/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopWarningDate: LocalDate? = null,

  @Schema(description = "Date which the hard stop period will start", example = "03/05/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val hardStopDate: LocalDate? = null,

  @Schema(description = "The date that the licence will start", example = "13/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val licenceStartDate: LocalDate? = null,

  @Schema(
    description = "The offenders release date, this will either be the conditional release if one exists or the actual release date otherwise",
    example = "12/12/2022",
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @Schema(description = "Is the prisoner due for early release")
  val isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,

  @Schema(description = "Is a review of this licence is required", example = "true")
  val isReviewNeeded: Boolean? = null,

  @Schema(description = "How this licence will need to be created", example = "PRISON_WILL_CREATE_THIS_LICENCE")
  val licenceCreationType: LicenceCreationType? = null,
)
