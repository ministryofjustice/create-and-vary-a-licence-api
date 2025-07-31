package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Request object for searching for offenders within a set of teams attached to a staff member")
data class ApproverSearchRequest(

  @field:Schema(description = "The prison caseloads of the prison staff member", example = "[BAI]")
  @field:NotEmpty
  val prisonCaseloads: List<String>,

  @field:Schema(
    description = "The query the user wishes to search for (e.g. CRN, name, NOMIS ID)",
    example = "Joe Bloggs",
  )
  val query: String,
)
