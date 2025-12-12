package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an enriched probation com search result")
data class ComSearchResponse(

  @field:Schema(description = "A list of probation search results")
  val results: List<FoundComCase>,

  @field:Schema(
    description = "Based on the search results, the number of results where an offender is in prison",
    example = "10",
  )
  val inPrisonCount: Int,

  @field:Schema(
    description = "Based on the search results, the number of results where an offender is on probation",
    example = "10",
  )
  val onProbationCount: Int,
)
