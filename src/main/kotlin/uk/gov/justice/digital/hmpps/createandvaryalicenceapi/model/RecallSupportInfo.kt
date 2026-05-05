package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType

@Schema(description = "Information about recall sentences to inform support for a prisoner")
data class RecallSupportInfo(
  @field:Schema(description = "The type of recall", example = "STANDARD")
  val recallType: RecallType = RecallType.NONE,

  @field:Schema(description = "The name of the recall", example = "Standard Recall")
  val recallName: String,

  @field:Schema(description = "Fixed term recall types ", example = "FTR_ORA")
  val fixTermSentenceTypes: List<String> = emptyList(),

  @field:Schema(description = "Standard recall types", example = "LR")
  val standardRecallSentenceTypes: List<String> = emptyList(),

  @field:Schema(description = "Other sentence types", example = "ADIMP_ORA]")
  val otherSentenceTypes: List<String> = emptyList(),
)
