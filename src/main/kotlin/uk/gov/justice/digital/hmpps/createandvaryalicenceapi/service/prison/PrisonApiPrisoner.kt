package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class OffenceHistory(
  val offenceDescription: String,
  val offenceCode: String,
  val mostSerious: Boolean,
)

data class SentenceDetail(
  @JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceStartDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceExpiryOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val confirmedReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewActualDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewEndDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var licenceExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var licenceExpiryOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionStartDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityOverrideDate: LocalDate? = null,
)

data class PrisonApiPrisoner(
  val offenderNo: String,
  val firstName: String,
  val middleName: String? = null,
  val lastName: String,
  val bookingId: Number,
  val legalStatus: String? = null,
  val offenceHistory: List<OffenceHistory>,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val dateOfBirth: LocalDate,

  val sentenceDetail: SentenceDetail,
)
