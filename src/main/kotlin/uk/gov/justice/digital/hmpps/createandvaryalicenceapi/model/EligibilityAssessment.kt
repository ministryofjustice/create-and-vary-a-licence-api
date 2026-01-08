package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind

data class EligibilityAssessment(
  @field:Schema(description = "A list of reasons the case is ineligible for any kind of CVL licence", example = "['A reason']")
  val genericIneligibilityReasons: List<String> = emptyList(),

  @field:Schema(description = "A list of reasons the case is ineligible for a CRD licence", example = "['A reason']")
  val crdIneligibilityReasons: List<String> = emptyList(),

  @field:Schema(description = "A list of reasons the case is ineligible for a PRRD licence", example = "['A reason']")
  val prrdIneligibilityReasons: List<String> = emptyList(),

  @field:Schema(description = "A list of reasons the case is ineligible for an HDC licence", example = "['A reason']")
  val hdcIneligibilityReasons: List<String> = emptyList(),

  @field:Schema(description = "A boolean denoting eligibility for CVL", example = "true")
  val isEligible: Boolean = genericIneligibilityReasons.isEmpty() &&
    (
      crdIneligibilityReasons.isEmpty() ||
        prrdIneligibilityReasons.isEmpty() ||
        hdcIneligibilityReasons.isEmpty()
      ),

  @field:Schema(description = "The kind of licence that the case is eligible for. Null if ineligible.", example = "CRD")
  val eligibleKind: LicenceKind? = when {
    !isEligible -> null
    crdIneligibilityReasons.isEmpty() -> LicenceKind.CRD
    hdcIneligibilityReasons.isEmpty() -> LicenceKind.HDC
    prrdIneligibilityReasons.isEmpty() -> LicenceKind.PRRD
    else -> null
  },

  @field:Schema(description = "A combined list of all of the reasons for ineligibility", example = "['A reason']")
  val ineligibilityReasons: List<String> = genericIneligibilityReasons + crdIneligibilityReasons + prrdIneligibilityReasons,
)
