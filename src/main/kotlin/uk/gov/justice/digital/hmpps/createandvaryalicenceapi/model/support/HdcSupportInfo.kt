package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.support

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus

@Schema(description = "Information about Hdc case to inform support for a prisoner")
data class HdcSupportInfo(
  @Schema(description = "Approval status of case")
  val approvalStatus: String? = null,
  @Schema(description = "Date decision was made")
  val approvalStatusDate: String? = null,
  @Schema(description = "Date checks were passed")
  val checksPassedDate: String? = null,
  @Schema(description = "Whether checks have been passed")
  val passed: Boolean = false,
  @Schema(description = "Reason case was refused")
  val refusedReason: String? = null,
) {
  constructor(status: PrisonerHdcStatus) : this(
    approvalStatus = status.approvalStatus,
    approvalStatusDate = status.approvalStatusDate,
    checksPassedDate = status.checksPassedDate,
    passed = status.passed,
    refusedReason = status.refusedReason,
  )
}
