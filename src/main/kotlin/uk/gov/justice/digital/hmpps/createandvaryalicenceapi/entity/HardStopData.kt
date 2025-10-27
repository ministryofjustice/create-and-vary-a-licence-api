package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDate

interface HardStopData {
  val licenceStartDate: LocalDate?
  val sentenceStartDate: LocalDate?
  val actualReleaseDate: LocalDate?
  val conditionalReleaseDate: LocalDate?
}
