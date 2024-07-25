package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Search Criteria for CA Caseload Search")
data class CaCaseloadSearch(
  @Schema(
    description = "Search text to filter caseload",
    example = "2022-04-20",
  )
  val searchString: String? = null,
  @Schema(
    description = "List of Prison Ids (can include OUT and TRN) to restrict the search by. Unrestricted if not supplied or null",
    example = "[\"MDI\"]",
  )
  val prisonCodes: Set<String>,
)
