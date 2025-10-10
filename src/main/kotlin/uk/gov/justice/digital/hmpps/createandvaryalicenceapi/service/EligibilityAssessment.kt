package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind

data class EligibilityAssessment(
  val genericIneligibilityReasons: List<String> = emptyList(),
  val crdIneligibilityReasons: List<String> = emptyList(),
  val prrdIneligibilityReasons: List<String> = emptyList(),
  val isEligible: Boolean,
  val eligibleKind: LicenceKind? = when {
    isEligible && crdIneligibilityReasons.isEmpty() -> LicenceKind.CRD
    isEligible && prrdIneligibilityReasons.isEmpty() -> LicenceKind.PRRD
    else -> null
  },
  val ineligiblityReasons: List<String> = genericIneligibilityReasons + crdIneligibilityReasons + prrdIneligibilityReasons,
)
