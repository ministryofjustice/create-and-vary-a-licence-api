package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.timeServed

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import java.time.LocalDate

@Schema(description = "Describes a Time Served case")
data class TimeServedCase(

  @field:Schema(description = "The full name of the person on licence", example = "John Doe")
  val name: String,

  @field:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonerNumber: String,

  @field:Schema(description = "The details for the active supervising probation officer")
  val probationPractitioner: ProbationPractitioner? = null,

  @field:Schema(description = "The date on which the prisoner leaves custody", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @field:Schema(description = "The sentence start date", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val sentenceStartDate: LocalDate? = null,

  @field:Schema(description = "The conditional release date", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDate: LocalDate? = null,

  @field:Schema(description = "The conditional override release date", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val conditionalReleaseDateOverride: LocalDate? = null,

  @field:Schema(description = "The confirmed release date", example = "30/11/2022")
  @field:JsonFormat(pattern = "dd/MM/yyyy")
  val confirmedReleaseDate: LocalDate? = null,

  @field:Schema(
    description = "Legal Status",
    example = "SENTENCED",
    allowableValues = ["RECALL", "DEAD", "INDETERMINATE_SENTENCE", "SENTENCED", "CONVICTED_UNSENTENCED", "CIVIL_PRISONER", "IMMIGRATION_DETAINEE", "REMAND", "UNKNOWN", "OTHER"],
  )
  var nomisLegalStatus: String? = null,

  @field:Schema(description = "The agency code where this offender resides or was released from", example = "MDI")
  val prisonCode: String? = null,

  @field:Schema(description = "This case is a time served based on CRDS rule", example = "true")
  val isTimeServedCaseByCrdsRule: Boolean,
  @field:Schema(description = "This case is a time served based on CRDS rule", example = "true")
  val isTimeServedCaseByNonCrdsRule: Boolean,
  @field:Schema(description = "This case is a time served based on All prison rule", example = "true")
  val isTimeServedCaseByAllPrisonRule: Boolean,

  @field:Schema(description = "This is a suspected time serve case", example = "true")
  val isTimeServedCase: Boolean = isTimeServedCaseByCrdsRule || isTimeServedCaseByNonCrdsRule || isTimeServedCaseByAllPrisonRule,

)
