package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.EnrichedProbationSearchResult

@Schema(description = "Describes an enriched probation search result")
data class ProbationSearchResult(

  @Schema(description = "A list of enriched probation search results")
  val results: List<EnrichedProbationSearchResult>,

  @Schema(description = "Based on the search results, the number of results where an offender is in prison", example = "10")
  val inPrisonCount: Int,

  @Schema(description = "Based on the search results, the number of results where an offender is on probation", example = "10")
  val onProbationCount: Int,
)
