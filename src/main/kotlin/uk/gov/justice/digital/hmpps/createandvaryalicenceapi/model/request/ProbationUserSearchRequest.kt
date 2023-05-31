package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for searching for offenders within a set of teams attached to a staff member")
data class ProbationUserSearchRequest(

  @Schema(description = "The query the user wishes to search for (e.g. CRN, name, NOMIS ID)", example = "Joe Bloggs")
  @field:NotBlank
  val query: String,

  @Schema(description = "The delius staff identifier of the probation staff member", example = "014829475")
  @field:NotNull
  val staffIdentifier: Long,
)
