package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.VaryApproverCase

@Schema(description = "Response object which describes a result from a vary approver caseload search")
data class VaryApproverCaseloadSearchResponse(
  @field:Schema(description = "A list of cases in a pdu search results")
  val pduCasesResponse: List<VaryApproverCase>,

  @field:Schema(description = "A list of cases in a region search results")
  val regionCasesResponse: List<VaryApproverCase>,
)
