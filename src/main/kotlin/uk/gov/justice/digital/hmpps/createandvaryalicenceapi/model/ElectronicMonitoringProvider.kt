package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus

@Schema(description = "Describes a electronic monitoring provider on a licence")
data class ElectronicMonitoringProvider(

  @Schema(description = "The internal ID for this electronic monitoring provider on this licence", example = "98987")
  val id: Long? = null,

  @Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long,

  @Schema(description = "Is the licence to be tagged for electronic monitoring programme")
  val isToBeTaggedForProgramme: Boolean? = null,

  @Schema(description = "Programme Name of the licence", example = "Off Some Road")
  val programmeName: String? = null,

  @Schema(description = "Electronic monitoring provider status", example = "NOT_STARTED")
  val status: ElectronicMonitoringProviderStatus? = null,
)
