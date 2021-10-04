package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Schema(description = "Response object which summarises a licence")
data class LicenceSummary (
  @Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long,

  @Schema(description = "Licence type code", example = "AP")
  val licenceType: LicenceType,

  @Schema(description = "The status of this licence", example = "IN_PROGRESS")
  val licenceStatus: LicenceStatus,
)
