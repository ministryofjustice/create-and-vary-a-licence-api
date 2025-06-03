package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an enriched prison case admin search result")
data class PrisonCaseAdminSearchResult(

  @Schema(description = "A list of offender in prison search results")
  val inPrisonResults: List<CaCase>,

  @Schema(description = "A list of offender on probation search results")
  val onProbationResults: List<CaCase>,
)
