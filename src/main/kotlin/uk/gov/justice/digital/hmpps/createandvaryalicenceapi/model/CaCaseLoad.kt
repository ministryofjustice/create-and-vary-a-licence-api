package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an CA(OMU) caseload")
data class CaCaseLoad(

  @Schema(description = "CA(OMU) cases")
  val cases: List<CaCase>,
)
