package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response

data class UserAccessResponse(
  val access: List<CaseAccessResponse>
)

data class CaseAccessResponse(
  val crn: String,
  val userExcluded: Boolean,
  val userRestricted: Boolean,
  val exclusionMessage: String? = null,
  val restrictionMessage: String? = null,
)
