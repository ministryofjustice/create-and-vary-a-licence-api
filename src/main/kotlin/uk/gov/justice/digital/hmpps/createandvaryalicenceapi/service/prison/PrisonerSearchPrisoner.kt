package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class SearchPaginationResponse(
  val content: List<PrisonerSearchPrisoner>,
)

data class PrisonerSearchPrisoner(
  val prisonerNumber: String,
  val pncNumber: String? = null,
  val bookingId: String,
  val status: String? = null,
  val mostSeriousOffence: String?,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val licenceExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewActualDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewEndDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val releaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val confirmedReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val actualParoleDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var releaseOnTemporaryLicenceDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseDate: LocalDate? = null,

  val legalStatus: String,

  val indeterminateSentence: Boolean,

  val imprisonmentStatus: String? = null,

  val imprisonmentStatusDescription: String? = null,

  val recall: Boolean,

  val prisonId: String,

  val locationDescription: String? = null,

  val prisonName: String? = null,

  val bookNumber: String,

  val firstName: String,

  val middleNames: String? = null,

  val lastName: String,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val dateOfBirth: LocalDate,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDateOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceStartDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionStartDate: LocalDate? = null,

  val croNumber: String? = null,
)
