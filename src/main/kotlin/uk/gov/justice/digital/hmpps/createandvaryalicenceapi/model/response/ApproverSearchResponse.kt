package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ApprovalCase

@Schema(description = "Response object which describes a result from an approver caseload search")
data class ApproverSearchResponse(
  @Schema(description = "A list of cases needing approval search results")
  val approvalNeededResponse: List<ApprovalCase>,

  @Schema(description = "A list of recently approved cases search results")
  val recentlyApprovedResponse: List<ApprovalCase>,
)
