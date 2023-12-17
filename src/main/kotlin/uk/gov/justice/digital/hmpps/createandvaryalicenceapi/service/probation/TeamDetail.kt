package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class TeamDetail(
  val code: String,
  val description: String,
  val borough: Detail,
  val district: Detail,
)
