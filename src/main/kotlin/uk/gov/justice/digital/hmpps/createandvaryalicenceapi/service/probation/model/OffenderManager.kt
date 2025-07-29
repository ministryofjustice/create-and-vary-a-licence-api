package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model

data class OffenderManager(
  val staffIdentifier: Long,
  val code: String?,
  val username: String?,
  val email: String?,
  val forename: String?,
  val surname: String?,
  val providerCode: String,
  val providerDescription: String?,
  val teamCode: String,
  val teamDescription: String,
  val boroughCode: String,
  val boroughDescription: String?,
  val districtCode: String,
  val districtDescription: String?,
  val crn: String,
)
