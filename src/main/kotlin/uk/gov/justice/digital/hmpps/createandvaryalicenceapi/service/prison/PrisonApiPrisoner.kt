package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class OffenceHistory(
  val offenceDescription: String,
  val offenceCode: String,
  val mostSerious: Boolean,
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

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDateOverrideDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val confirmedReleaseDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewActualDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionStartDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityDate: LocalDate? = null,
)
