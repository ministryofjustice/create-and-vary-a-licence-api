package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Information to populate the support page for a prisoner")
data class SupportInfo(
  @field:Schema(description = "Indicates if the case is an IS91 case", example = "false")
  val isIS91Case: Boolean? = false,
  @field:Schema(description = "Information about recall sentences to inform support for a prisoner")
  val recallSupportInfo: RecallSupportInfo? = null,
  @field:Schema(description = "Information about remand cases to inform support for a prisoner")
  val remandSupportInfo: RemandSupportInfo? = null,
)
