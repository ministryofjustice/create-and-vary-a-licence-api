package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.determineReleaseDateKind
import java.time.LocalDate

interface SentenceDateHolder {
  val licenceStartDate: LocalDate?
  val conditionalReleaseDate: LocalDate?
  val actualReleaseDate: LocalDate?
  val homeDetentionCurfewActualDate: LocalDate?
    get() = null
  val postRecallReleaseDate: LocalDate?
  val latestReleaseDate: LocalDate?
    get() = if (determineReleaseDateKind(postRecallReleaseDate, conditionalReleaseDate) == LicenceKind.PRRD) postRecallReleaseDate else conditionalReleaseDate
}
