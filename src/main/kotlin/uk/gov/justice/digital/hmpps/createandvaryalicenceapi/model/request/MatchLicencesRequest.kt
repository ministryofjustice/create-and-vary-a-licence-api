package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Request object for searching licences by field")
data class MatchLicencesRequest(

  @field:Schema(description = "A list of licence status codes", example = "['ACTIVE', 'APPROVED']")
  val status: List<LicenceStatus>? = null,

  @field:Schema(description = "A list of NOMIS ID's", example = "['B76546GH', 'Y76499GY']")
  val nomsId: List<String>? = null,

)
