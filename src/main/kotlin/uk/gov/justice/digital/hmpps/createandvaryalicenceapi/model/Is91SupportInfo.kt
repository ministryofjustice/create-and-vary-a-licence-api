package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Information about IS91 cases to inform support for a prisoner")
data class Is91SupportInfo(
  @field:Schema(description = "Indicates if the case is an IS91 case", example = "false")
  val isIs91Case: Boolean,
  val is91CourtEventOutcomes: List<Is91CourtEventOutcome> = emptyList(),
) {
  constructor(outcomes: List<Is91CourtEventOutcome>) : this(outcomes.isNotEmpty(), outcomes)

  @Schema(description = "Information about Is91 cases to inform support for a prisoner")
  data class Is91CourtEventOutcome(
    @field:Schema(description = "The court event outcome code", example = "3006")
    val courtEventOutcomeCode: String? = null,
    @field:Schema(description = "The court event outcome description", example = "Deportation recommended")
    val courtEventOutcomeDescription: String? = null,
  )
}
