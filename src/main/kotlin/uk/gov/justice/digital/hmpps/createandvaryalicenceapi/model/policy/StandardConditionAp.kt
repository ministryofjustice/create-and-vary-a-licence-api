package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import io.swagger.v3.oas.annotations.media.Schema

data class StandardConditionAp(
  @Schema(description = "The unique code for this standard AP condition", example = "9ce9d594-e346-4785-9642-c87e764bee37")
  override var code: String,
  @Schema(description = "The text of this standard AP condition", example = "Be of generally good behaviour")
  override val text: String,
  override val requiresInput: Boolean,
  override val tpl: String?
) : ILicenceCondition
