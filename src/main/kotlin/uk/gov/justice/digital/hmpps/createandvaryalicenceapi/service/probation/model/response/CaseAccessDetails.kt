package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response

data class CaseAccessDetails(
  val type: CaseAccessRestrictionType,
  val message: String? = null,
)

enum class CaseAccessRestrictionType {
  RESTRICTED,
  EXCLUDED,
  NONE,
}
