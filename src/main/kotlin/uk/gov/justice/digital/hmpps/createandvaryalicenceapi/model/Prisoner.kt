package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Prisoner(
  @field:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String? = null,

  @field:Schema(description = "PNC Number", example = "12/394773H")
  val pncNumber: String? = null,

  @field:Schema(description = "CRO Number", example = "29906/12J")
  val croNumber: String? = null,

  @field:Schema(description = "Booking No.", example = "0001200924")
  val bookingId: String? = null,

  @field:Schema(description = "Book Number", example = "38412A")
  val bookNumber: String? = null,

  @field:Schema(required = true, description = "First Name", example = "Joe")
  val firstName: String? = null,

  @field:Schema(description = "Middle Names", example = "John James")
  val middleNames: String? = null,

  @field:Schema(required = true, description = "Last name", example = "Bloggs")
  val lastName: String? = null,

  @field:Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  val dateOfBirth: LocalDate? = null,

  @field:Schema(required = true, description = "Status of the prisoner", example = "ACTIVE IN")
  val status: String? = null,

  @field:Schema(description = "In/Out Status", example = "IN", allowableValues = ["IN", "OUT", "TRN"])
  val inOutStatus: String? = null,

  @field:Schema(description = "Prison ID", example = "MDI")
  val prisonId: String? = null,

  @field:Schema(description = "Location Description", example = "Outside - released from Leeds")
  val locationDescription: String? = null,

  @field:Schema(description = "Prison Name", example = "HMP Leeds")
  val prisonName: String? = null,

  @field:Schema(
    description = "Legal Status",
    example = "SENTENCED",
    allowableValues = ["RECALL", "DEAD", "INDETERMINATE_SENTENCE", "SENTENCED", "CONVICTED_UNSENTENCED", "CIVIL_PRISONER", "IMMIGRATION_DETAINEE", "REMAND", "UNKNOWN", "OTHER"],
  )
  val legalStatus: String? = null,

  @field:Schema(description = "The prisoner's imprisonment status code.", example = "LIFE")
  val imprisonmentStatus: String? = null,

  @field:Schema(description = "The prisoner's imprisonment status description.", example = "Serving Life Imprisonment")
  val imprisonmentStatusDescription: String? = null,

  @field:Schema(required = true, description = "Most serious offence for this sentence", example = "Robbery")
  val mostSeriousOffence: String? = null,

  @field:Schema(description = "Indicates that the prisoner has been recalled", example = "false")
  val recall: Boolean? = null,

  @field:Schema(description = "Indicates that the prisoner has an indeterminate sentence", example = "true")
  val indeterminateSentence: Boolean? = null,

  @field:Schema(description = "Start Date for this sentence", example = "2020-04-03")
  val sentenceStartDate: LocalDate? = null,

  @field:Schema(description = "Actual of most likely Release Date", example = "2023-05-02")
  val releaseDate: LocalDate? = null,

  @field:Schema(description = "Release Date Confirmed", example = "2023-05-01")
  val confirmedReleaseDate: LocalDate? = null,

  @field:Schema(description = "Sentence Expiry Date", example = "2023-05-01")
  val sentenceExpiryDate: LocalDate? = null,

  @field:Schema(description = "Licence Expiry Date", example = "2023-05-01")
  val licenceExpiryDate: LocalDate? = null,

  @field:Schema(description = "HDC Eligibility Date", example = "2023-05-01")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @field:Schema(description = "HDC Actual Date", example = "2023-05-01")
  val homeDetentionCurfewActualDate: LocalDate? = null,

  @field:Schema(description = "HDC End Date", example = "2023-05-02")
  val homeDetentionCurfewEndDate: LocalDate? = null,

  @field:Schema(description = "Top-up supervision start date", example = "2023-04-29")
  val topupSupervisionStartDate: LocalDate? = null,

  @field:Schema(description = "Top-up supervision expiry date", example = "2023-05-01")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @field:Schema(description = "Parole  Eligibility Date", example = "2023-05-01")
  val paroleEligibilityDate: LocalDate? = null,

  @field:Schema(
    description = "Post Recall Release Date. if postRecallReleaseOverrideDate is available then it will be set as postRecallReleaseDate",
    example = "2023-05-01",
  )
  val postRecallReleaseDate: LocalDate? = null,

  @field:Schema(
    description = "Conditional Release Date. If conditionalReleaseOverrideDate is available then it will be set as conditionalReleaseDate",
    example = "2023-05-01",
  )
  val conditionalReleaseDate: LocalDate? = null,

  @field:Schema(description = "Actual Parole Date", example = "2023-05-01")
  val actualParoleDate: LocalDate? = null,

  @field:Schema(description = "Release on Temporary Licence Date", example = "2023-05-01")
  val releaseOnTemporaryLicenceDate: LocalDate? = null,
)

fun Prisoner?.isBreachOfTopUpSupervision(): Boolean = this != null && this.imprisonmentStatus == "BOTUS"
