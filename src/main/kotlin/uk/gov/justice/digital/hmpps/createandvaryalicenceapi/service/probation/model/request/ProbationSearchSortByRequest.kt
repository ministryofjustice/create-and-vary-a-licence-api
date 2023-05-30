package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request

data class ProbationSearchSortByRequest(
  val field: String = "name.forename",
  val direction: String = "asc"
)
