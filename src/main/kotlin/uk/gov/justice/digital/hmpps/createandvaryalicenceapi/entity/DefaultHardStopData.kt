package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import java.time.LocalDate

data class DefaultHardStopData(
  override val licenceStartDate: LocalDate?,
  override val sentenceStartDate: LocalDate?,
  override val actualReleaseDate: LocalDate?,
  override val conditionalReleaseDate: LocalDate?
) : HardStopData
