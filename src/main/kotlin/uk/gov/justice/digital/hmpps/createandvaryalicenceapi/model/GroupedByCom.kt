package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an CA(OMU) case")
data class GroupedByCom(
  var withStaffCode: List<CaCase>,
  var withStaffUsername: List<CaCase>,
  var withNoComId: List<CaCase>,
)