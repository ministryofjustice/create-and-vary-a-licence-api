package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

data class ProbationUserSearchSortBy(val field: SearchField, val direction: SearchDirection)

enum class SearchField(val probationSearchApiSortType: String) {
  FORENAME("name.forename"),
  SURNAME("name.surname"),
  CRN("identifiers.crn"),
  COM_FORENAME("manager.name.forename"),
  COM_SURNAME("manager.name.surname"),
}

enum class SearchDirection {
  ASC,
  DESC,
}
