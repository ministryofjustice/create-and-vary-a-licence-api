package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class PrisonerSearchPrisoner(
  val prisonerNumber: String,
  val bookingId: Long,
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
  val paroleEligibilityDate: LocalDate? = null,

  val legalStatus: String,

  val indeterminateSentence: Boolean,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val actualParoleDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseDate: LocalDate? = null,

)
