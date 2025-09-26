package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDate

interface SentenceDateHolder {
  val licenceStartDate: LocalDate?
  val conditionalReleaseDate: LocalDate?
  val actualReleaseDate: LocalDate?
  val homeDetentionCurfewActualDate: LocalDate?
    get() = null
  val postRecallReleaseDate: LocalDate?
}
