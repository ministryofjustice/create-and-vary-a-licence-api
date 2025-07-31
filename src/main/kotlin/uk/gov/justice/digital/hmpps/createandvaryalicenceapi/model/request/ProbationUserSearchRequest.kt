package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ProbationSearchSortBy

@Schema(description = "Request object for searching for offenders within a set of teams attached to a staff member")
data class ProbationUserSearchRequest(

  @field:Schema(
    description = "The query the user wishes to search for (e.g. CRN, name, NOMIS ID)",
    example = "Joe Bloggs",
  )
  val query: String,

  @field:Schema(description = "The delius staff identifier of the probation staff member", example = "014829475")
  @field:NotNull
  val staffIdentifier: Long,

  @field:Schema(
    description = "A list of fields to sort by along with the sort direction for each",
  )
  val sortBy: List<ProbationSearchSortBy> = emptyList(),
)
