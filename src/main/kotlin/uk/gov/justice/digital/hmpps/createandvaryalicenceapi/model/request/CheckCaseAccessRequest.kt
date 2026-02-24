package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """"Request object for check whether a user has access to a case. One of crn, nomisId or licenceId must be provided.
      If multiple are provided the first provided will be used to check case access in this order, crd, nomisId, licenceId.""",
)
data class CheckCaseAccessRequest(
  @field:Schema(description = "The case reference number", example = "X12444")
  val crn: String? = null,

  @field:Schema(description = "The nomis number associated with the case", example = "A1234AA")
  val nomisId: String? = null,

  @field:Schema(description = "The id of the licence to check access to", example = "8947")
  val licenceId: Long? = null,
)
