package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

class PreviousConviction(
  val convictionDate: String?,
  val detail: Map<String, String>? = mutableMapOf(),
)
