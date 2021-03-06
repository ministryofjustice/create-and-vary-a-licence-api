package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Request object for searching licences by field")
data class MatchLicencesRequest(

  @Schema(description = "A list of prison codes", example = "['PVI', 'BAI']")
  val prison: List<String>? = null,

  @Schema(description = "A list of licence status codes", example = "['ACTIVE', 'APPROVED']")
  val status: List<LicenceStatus>? = null,

  @Schema(description = "A list of staff identifiers - the responsible probation officer", example = "[1234, 4321]")
  val staffId: List<Int>? = null,

  @Schema(description = "A list of NOMIS ID's", example = "['B76546GH', 'Y76499GY']")
  val nomsId: List<String>? = null,

  @Schema(description = "A list of probation delivery unit codes", example = "['N55', 'P66']")
  val pdu: List<String>? = null,

)
