package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchSortByRequest

@Schema(description = "Request object for searching for offenders within a set of teams attached to a staff member")
data class LicenceCaseloadSearchRequest(
  @Schema(description = "A list of team codes for a given staff member", example = "['A12B34']")
  @field:NotBlank
  val teamCodes: List<String>,

  @Schema(description = "The query string (e.g. CRN number, name, NOMIS id", example = "014829475")
  @field:NotNull
  val query: String,

  @Schema(description = "The sorting parameters (e.g. by crn, name, manager name)", example = "name.forename")
  @field:NotNull
  val sortBy: ProbationSearchSortByRequest
)
