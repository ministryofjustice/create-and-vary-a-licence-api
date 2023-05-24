package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the sort condition for the probation search")
data class ProbationSearchSortByRequest (
  val field: String = "name.forename",
  val direction: String = "asc"
)
