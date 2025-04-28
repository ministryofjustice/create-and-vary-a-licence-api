package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import org.springframework.data.domain.Sort

data class ProbationSearchSortBy(val field: SearchField, val direction: Sort.Direction)

enum class SearchField(val probationSearchApiSortType: String) {
  FORENAME("firstName"),
  SURNAME("surname"),
  CRN("crn"),
  COM_FORENAME("staff.forename"),
  COM_SURNAME("staff.surname"),
}
