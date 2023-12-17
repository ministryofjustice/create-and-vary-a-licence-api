package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class PrisonerSearchPrisoner(
  val prisonerNumber: String,
  val bookingId: String,
  val status: String? = null,
  val mostSeriousOffence: String?,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val licenceExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topUpSupervisionExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

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
  val postRecallReleaseDate: LocalDate? = null,

  val legalStatus: String,

  val indeterminateSentence: Boolean,

  val recall: Boolean,

  val prisonId: String,

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
  val topUpSupervisionStartDate: LocalDate? = null,

  val croNumber: String? = null,
)
