package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

@Schema(description = "Describes a probation search result")
data class ProbationSearchResult(
  @Schema(description = "The forename and surname of the offender")
  val name: String = "",

  @Schema(description = "The forename and surname of the COM")
  val comName: String = "",

  @Schema(description = "The description of the COM's team", example = "Test Team")
  val teamName: String? = "",

  @Schema(description = "The release date of the offender", example = "27/07/2023")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val releaseDate: LocalDate? = null,

  @Schema(description = "The status of the licence", example = "IN_PROGRESS")
  val licenceStatus: LicenceStatus? = null,

  @Schema(description = "Indicates whether the offender is out on probation based on their licence", example = "true")
  val isOnProbation: Boolean? = null,
)
