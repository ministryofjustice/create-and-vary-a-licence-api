package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Information about remand cases to inform support for a prisoner")
data class RemandSupportInfo(
  @field:Schema(description = "Indicates if the case is a remand case", example = "false")
  val isRemand: Boolean = false,

  @property:Deprecated("to be replaced with list instead")
  @field:Schema(description = "The court event outcome code", example = "4531")
  val courtEventOutcomeCode: String? = null,

  @property:Deprecated("to be replaced with list instead")
  @field:Schema(description = "The court event outcome description", example = "Remand in Custody (Bail Refused)")
  val courtEventOutcomeDescription: String? = null,

  @field:Schema(description = "All court event outcomes related to remand case")
  val remandCourtEventOutcomes: List<RemandCourtEventOutcome>? = null,

) {
  constructor(remandCourtEventOutcomes: List<RemandCourtEventOutcome>) : this(
    isRemand = remandCourtEventOutcomes.isNotEmpty(),
    courtEventOutcomeCode = remandCourtEventOutcomes.firstOrNull()?.courtEventOutcomeCode,
    courtEventOutcomeDescription = remandCourtEventOutcomes.firstOrNull()?.courtEventOutcomeDescription,
    remandCourtEventOutcomes = remandCourtEventOutcomes,
  )

  @Schema(description = "Information about remand cases to inform support for a prisoner")
  data class RemandCourtEventOutcome(
    @field:Schema(description = "The court event outcome code", example = "4531")
    val courtEventOutcomeCode: String? = null,
    @field:Schema(description = "The court event outcome description", example = "Remand in Custody (Bail Refused)")
    val courtEventOutcomeDescription: String? = null,
  )
}
