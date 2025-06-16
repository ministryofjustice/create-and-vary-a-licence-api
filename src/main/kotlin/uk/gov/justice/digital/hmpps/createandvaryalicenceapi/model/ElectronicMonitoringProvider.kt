package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a electronic monitoring provider on a licence")
data class ElectronicMonitoringProvider(

  @Schema(description = "Is the licence to be tagged for electronic monitoring programme")
  val isToBeTaggedForProgramme: Boolean? = null,

  @Schema(description = "Programme Name of the licence", example = "Off Some Road")
  val programmeName: String? = null,
)
