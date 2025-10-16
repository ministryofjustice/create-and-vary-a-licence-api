package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

data class CvlRecord(
  val nomisId: String,
  val licenceStartDate: LocalDate? = null,
  val isEligible: Boolean = false,
  val eligibleKind: LicenceKind? = null,
  val ineligiblityReasons: List<String> = emptyList(),
  val hardstopKind: LicenceKind? = null,
)
