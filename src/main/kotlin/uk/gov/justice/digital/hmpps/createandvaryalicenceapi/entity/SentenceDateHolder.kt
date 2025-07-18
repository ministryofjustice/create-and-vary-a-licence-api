package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

interface SentenceDateHolder {
  val kind: LicenceKind
  val licenceStartDate: LocalDate?
  val conditionalReleaseDate: LocalDate?
  val actualReleaseDate: LocalDate?
  val homeDetentionCurfewActualDate: LocalDate?
    get() = null
  val postRecallReleaseDate: LocalDate?
  val latestReleaseDate: LocalDate?
    get() = if (kind == LicenceKind.PRRD) postRecallReleaseDate else conditionalReleaseDate
}
