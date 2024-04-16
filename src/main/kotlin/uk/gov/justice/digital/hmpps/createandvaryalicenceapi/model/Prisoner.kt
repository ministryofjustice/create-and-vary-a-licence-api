package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Prisoner(
  @get:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  var prisonerNumber: String? = null,

  @get:Schema(description = "PNC Number", example = "12/394773H")
  var pncNumber: String? = null,

  @get:Schema(description = "CRO Number", example = "29906/12J")
  var croNumber: String? = null,

  @get:Schema(description = "Booking No.", example = "0001200924")
  var bookingId: String? = null,

  @get:Schema(description = "Book Number", example = "38412A")
  var bookNumber: String? = null,

  @get:Schema(required = true, description = "First Name", example = "Robert")
  var firstName: String? = null,

  @get:Schema(description = "Middle Names", example = "John James")
  var middleNames: String? = null,

  @get:Schema(required = true, description = "Last name", example = "Larsen")
  var lastName: String? = null,

  @get:Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  var dateOfBirth: LocalDate? = null,

  @get:Schema(required = true, description = "Status of the prisoner", example = "ACTIVE IN")
  var status: String? = null,

  @get:Schema(description = "In/Out Status", example = "IN", allowableValues = ["IN", "OUT", "TRN"])
  var inOutStatus: String? = null,

  @get:Schema(description = "Prison ID", example = "MDI")
  var prisonId: String? = null,

  @get:Schema(description = "Location Description", example = "Outside - released from Leeds")
  var locationDescription: String? = null,

  @get:Schema(description = "Prison Name", example = "HMP Leeds")
  var prisonName: String? = null,

  @get:Schema(
    description = "Legal Status",
    example = "SENTENCED",
    allowableValues = ["RECALL", "DEAD", "INDETERMINATE_SENTENCE", "SENTENCED", "CONVICTED_UNSENTENCED", "CIVIL_PRISONER", "IMMIGRATION_DETAINEE", "REMAND", "UNKNOWN", "OTHER"],
  )
  var legalStatus: String? = null,

  @get:Schema(description = "The prisoner's imprisonment status code.", example = "LIFE")
  var imprisonmentStatus: String? = null,

  @get:Schema(description = "The prisoner's imprisonment status description.", example = "Serving Life Imprisonment")
  var imprisonmentStatusDescription: String? = null,

  @get:Schema(required = true, description = "Most serious offence for this sentence", example = "Robbery")
  var mostSeriousOffence: String? = null,

  @get:Schema(description = "Indicates that the prisoner has been recalled", example = "false")
  var recall: Boolean? = null,

  @get:Schema(description = "Indicates that the prisoner has an indeterminate sentence", example = "true")
  var indeterminateSentence: Boolean? = null,

  @get:Schema(description = "Start Date for this sentence", example = "2020-04-03")
  var sentenceStartDate: LocalDate? = null,

  @get:Schema(description = "Actual of most likely Release Date", example = "2023-05-02")
  var releaseDate: LocalDate? = null,

  @get:Schema(description = "Release Date Confirmed", example = "2023-05-01")
  var confirmedReleaseDate: LocalDate? = null,

  @get:Schema(description = "Sentence Expiry Date", example = "2023-05-01")
  var sentenceExpiryDate: LocalDate? = null,

  @get:Schema(description = "Licence Expiry Date", example = "2023-05-01")
  var licenceExpiryDate: LocalDate? = null,

  @get:Schema(description = "HDC Eligibility Date", example = "2023-05-01")
  var homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @get:Schema(description = "HDC Actual Date", example = "2023-05-01")
  var homeDetentionCurfewActualDate: LocalDate? = null,

  @get:Schema(description = "HDC End Date", example = "2023-05-02")
  var homeDetentionCurfewEndDate: LocalDate? = null,

  @get:Schema(description = "Top-up supervision start date", example = "2023-04-29")
  var topupSupervisionStartDate: LocalDate? = null,

  @get:Schema(description = "Top-up supervision expiry date", example = "2023-05-01")
  var topupSupervisionExpiryDate: LocalDate? = null,

  @get:Schema(description = "Parole  Eligibility Date", example = "2023-05-01")
  var paroleEligibilityDate: LocalDate? = null,

  @get:Schema(
    description = "Post Recall Release Date. if postRecallReleaseOverrideDate is available then it will be set as postRecallReleaseDate",
    example = "2023-05-01",
  )
  var postRecallReleaseDate: LocalDate? = null,

  @get:Schema(
    description = "Conditional Release Date. If conditionalReleaseOverrideDate is available then it will be set as conditionalReleaseDate",
    example = "2023-05-01",
  )
  var conditionalReleaseDate: LocalDate? = null,

  @get:Schema(description = "Actual Parole Date", example = "2023-05-01")
  var actualParoleDate: LocalDate? = null,

  @get:Schema(description = "Release on Temporary Licence Date", example = "2023-05-01")
  var releaseOnTemporaryLicenceDate: LocalDate? = null,
)
