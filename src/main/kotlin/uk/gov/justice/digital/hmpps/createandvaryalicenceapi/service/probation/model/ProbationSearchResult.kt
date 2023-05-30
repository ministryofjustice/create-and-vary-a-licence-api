package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a probation search result")
data class ProbationSearchResult(
  @Schema(description = "The forename and surname of the offender")
  val name: String = "",

  @Schema(description = "The forename and surname of the COM")
  val comName: String = "",

  @Schema(description = "The identifier for the COM")
  val comCode: String = "",
)
