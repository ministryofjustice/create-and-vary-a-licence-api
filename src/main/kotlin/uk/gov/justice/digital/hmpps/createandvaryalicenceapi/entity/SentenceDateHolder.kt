package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDate

interface SentenceDateHolder {
  val licenceStartDate: LocalDate?
  val sentenceStartDate: LocalDate?
  val conditionalReleaseDate: LocalDate?
  val actualReleaseDate: LocalDate?

  // HDC actual date is not applicable for CRD and PRRD licences and so defaults to null
  // For PrisonerSearchPrisoner entity this is overridden to provide the actual value (this may not mean the case is a HDC licence)
  val homeDetentionCurfewActualDate: LocalDate?
    get() = null
  val postRecallReleaseDate: LocalDate?
}
