package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class OtherIds(
  val crn: String,
  val croNumber: String? = null,
  val pncNumber: String? = null,
)
