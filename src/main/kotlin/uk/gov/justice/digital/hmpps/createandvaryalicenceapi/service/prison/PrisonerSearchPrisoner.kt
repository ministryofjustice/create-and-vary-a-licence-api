package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import com.fasterxml.jackson.annotation.JsonFormat
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.DefaultHardStopData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HardStopData
import java.time.LocalDate

data class PrisonerSearchPrisoner(
  val prisonerNumber: String,
  val pncNumber: String? = null,
  val bookingId: String? = null,
  val status: String? = null,
  val mostSeriousOffence: String?,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val licenceExpiryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionExpiryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewActualDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var homeDetentionCurfewEndDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val releaseDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val confirmedReleaseDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val paroleEligibilityDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val actualParoleDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  var releaseOnTemporaryLicenceDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val postRecallReleaseDate: LocalDate? = null,

  val legalStatus: String? = null,

  val indeterminateSentence: Boolean? = null,

  val imprisonmentStatus: String? = null,

  val imprisonmentStatusDescription: String? = null,

  val recall: Boolean? = null,

  val prisonId: String? = null,

  val locationDescription: String? = null,

  val prisonName: String? = null,

  val bookNumber: String? = null,

  val firstName: String,

  val middleNames: String? = null,

  val lastName: String,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val dateOfBirth: LocalDate,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val conditionalReleaseDateOverrideDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceStartDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val sentenceExpiryDate: LocalDate? = null,

  @field:JsonFormat(pattern = "yyyy-MM-dd")
  val topupSupervisionStartDate: LocalDate? = null,

  val croNumber: String? = null,

) {

  fun fullName() = listOfNotNull(firstName, middleNames, lastName).filter { it.isNotBlank() }.joinToString(" ")

  fun toHardStopData(licenceStartDate: LocalDate?): HardStopData = DefaultHardStopData(
    licenceStartDate = licenceStartDate,
    sentenceStartDate = this.sentenceStartDate,
    actualReleaseDate = this.confirmedReleaseDate,
    conditionalReleaseDate = this.conditionalReleaseDateOverrideDate ?: this.conditionalReleaseDate
  )
}
