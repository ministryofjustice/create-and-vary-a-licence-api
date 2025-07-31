package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(description = "Search Criteria for Release Date Search")
data class ReleaseDateSearch(
  @field:Schema(
    description = "The lower bound for the release date range of which to search - defaults to today if not provided",
    example = "2022-04-20",
  )
  val earliestReleaseDate: LocalDate? = LocalDate.now(),
  @field:Schema(
    description = "The upper bound for the release date range of which to search. A required field.",
    example = "2022-05-20",
  )
  @field:NotNull(message = "Invalid search - latestReleaseDateRange is a required field")
  val latestReleaseDate: LocalDate?,
  @field:Schema(
    description = "List of Prison Ids (can include OUT and TRN) to restrict the search by. Unrestricted if not supplied or null",
    example = "[\"MDI\"]",
  )
  val prisonIds: Set<String>? = null,
)
