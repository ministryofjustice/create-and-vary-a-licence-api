package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import java.time.LocalDate

data class SentenceDates(
  val conditionalReleaseDate: LocalDate? = null,
  val actualReleaseDate: LocalDate? = null,
  val sentenceStartDate: LocalDate? = null,
  val sentenceEndDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val topupSupervisionStartDate: LocalDate? = null,
  val topupSupervisionExpiryDate: LocalDate? = null,
  val postRecallReleaseDate: LocalDate? = null,
  val homeDetentionCurfewActualDate: LocalDate? = null,
  val homeDetentionCurfewEndDate: LocalDate? = null,
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,
)
