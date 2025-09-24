package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an enriched prison case admin search result")
data class PrisonCaseAdminSearchResult(

  @field:Schema(description = "A list of offenders that are in prison")
  val inPrisonResults: List<CaCase>,

  @field:Schema(description = "A list of offenders that are on probation")
  val onProbationResults: List<CaCase>,

  @field:Schema(description = "A list of offenders that require attention")
  val attentionNeededResults: List<CaCase>,
)
