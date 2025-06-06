package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED

@Schema(description = "Search criteria for vary approver caseload search")
data class VaryApproverCaseloadSearchRequest(
  @Schema(
    description = "The probation delivery units where the the licence is supervised",
    example = "[\"N55PDV\"]",
    requiredMode = NOT_REQUIRED,
  )
  val probationPduCodes: List<String>? = null,

  @Schema(
    description = "The probation region where the licence is supervised",
    example = "N01",
    requiredMode = NOT_REQUIRED,
  )
  val probationAreaCode: String? = null,

  @Schema(
    description = "Search text to filter caseload",
    example = "2022-04-20",
    requiredMode = NOT_REQUIRED,
  )
  val searchTerm: String? = null,
)
