package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Information about remand cases to inform support for a prisoner")
data class RemandSupportInfo(
  @field:Schema(description = "Indicates if the case is a remand case", example = "false")
  val isRemand: Boolean = false,

  @field:Schema(description = "The court event outcome code", example = "4531")
  val courtEventOutcomeCode: String? = null,

  @field:Schema(description = "The court event outcome description", example = "Remand in Custody (Bail Refused)")
  val courtEventOutcomeDescription: String? = null,
)
