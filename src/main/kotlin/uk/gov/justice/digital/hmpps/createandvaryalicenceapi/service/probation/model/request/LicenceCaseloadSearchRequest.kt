package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request

data class LicenceCaseloadSearchRequest(
  val teamCodes: List<String>,
  val query: String,
  val sortBy: ProbationSearchSortByRequest,
)
