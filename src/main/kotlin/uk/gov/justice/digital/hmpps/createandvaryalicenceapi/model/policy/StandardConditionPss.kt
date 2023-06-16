package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

import io.swagger.v3.oas.annotations.media.Schema

data class StandardConditionPss(
  @Schema(description = "The unique code for this standard PSS condition", example = "9ce9d594-e346-4785-9642-c87e764bee37")
  override var code: String,
  @Schema(description = "The text of this standard PSS condition", example = "Be of generally good behaviour")
  override val text: String,
  override val tpl: String? = null,
) : ILicenceCondition
