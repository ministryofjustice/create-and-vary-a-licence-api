package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.support

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment

@Schema(description = "Information to populate the support page for a prisoner")
data class SupportInfo(
  @property:Deprecated("use #is91SupportInfo instead")
  @field:Schema(description = "Indicates if the case is an IS91 case", example = "false")
  val isIS91Case: Boolean?,
  @field:Schema(description = "Information about recall sentences to inform support for a prisoner")
  val recallSupportInfo: RecallSupportInfo?,
  @field:Schema(description = "Information about remand cases to inform support for a prisoner")
  val remandSupportInfo: RemandSupportInfo?,
  @field:Schema(description = "Information about IS91 cases to inform support for a prisoner")
  val is91SupportInfo: Is91SupportInfo?,
  @field:Schema(description = "Information about HDC case info in Nomis to inform support for a prisoner")
  val nomisHdcSupportInfo: HdcSupportInfo?,
  @field:Schema(description = "Information about Eligibility assessment")
  val eligibilityAssessment: EligibilityAssessment?,
)
