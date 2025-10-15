package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.SentenceDates
import java.time.LocalDate

data class OffenceHistory(
  val offenceDescription: String,
  val offenceCode: String,
  val mostSerious: Boolean,
)

data class SentenceDetail(
  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceStartDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceExpiryOverrideDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceExpiryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseOverrideDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val confirmedReleaseDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewActualDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewEndDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var licenceExpiryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var licenceExpiryOverrideDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionStartDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryOverrideDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseOverrideDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityOverrideDate: LocalDate? = null,
) {
  fun toSentenceDates() = SentenceDates(
    conditionalReleaseDate = conditionalReleaseOverrideDate ?: conditionalReleaseDate,
    actualReleaseDate = confirmedReleaseDate,
    sentenceStartDate = sentenceStartDate,
    sentenceEndDate = sentenceExpiryOverrideDate ?: sentenceExpiryDate,
    licenceExpiryDate = licenceExpiryOverrideDate ?: licenceExpiryDate,
    topupSupervisionStartDate = topupSupervisionStartDate,
    topupSupervisionExpiryDate = topupSupervisionExpiryOverrideDate ?: topupSupervisionExpiryDate,
    postRecallReleaseDate = postRecallReleaseOverrideDate ?: postRecallReleaseDate,
    homeDetentionCurfewEligibilityDate = homeDetentionCurfewEligibilityDate,
    homeDetentionCurfewActualDate = homeDetentionCurfewActualDate,
    homeDetentionCurfewEndDate = homeDetentionCurfewEndDate,
  )
}

data class PrisonApiPrisoner(
  val offenderNo: String,
  val firstName: String,
  val middleName: String? = null,
  val lastName: String,
  val bookingId: Number,
  val legalStatus: String? = null,
  val offenceHistory: List<OffenceHistory>,
  val agencyId: String,
  val status: String,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val dateOfBirth: LocalDate,

  val sentenceDetail: SentenceDetail,
)
