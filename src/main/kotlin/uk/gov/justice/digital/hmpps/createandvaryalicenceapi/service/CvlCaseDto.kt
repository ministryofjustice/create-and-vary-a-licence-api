package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate

data class CvlCaseDto(
  val nomisId: String,
  val licenceStartDate: LocalDate? = null,
  val isEligible: Boolean = false,
  val eligibleKind: LicenceKind? = null,
  val ineligiblityReasons: EligibilityAssessment,
)

data class EligibilityAssessment(
  val genericIneligibilityReasons: List<String> = emptyList(),
  val crdIneligibilityReasons: List<String> = emptyList(),
  val prrdIneligibilityReasons: List<String> = emptyList(),
  val isEligible: Boolean,
  val eligibleKind: LicenceKind? = when (isEligible) {
    true if crdIneligibilityReasons.isEmpty() -> LicenceKind.CRD
    true if prrdIneligibilityReasons.isEmpty() -> LicenceKind.PRRD
    else -> null
  },
)
