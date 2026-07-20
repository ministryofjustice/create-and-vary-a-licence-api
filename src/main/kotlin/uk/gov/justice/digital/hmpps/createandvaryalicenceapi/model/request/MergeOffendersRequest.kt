package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for merging offenders")
data class MergeOffendersRequest(
  @field:Schema(description = "The nomis id being replaced", example = "G4268VE")
  val oldNomisId: String,

  @field:Schema(description = "The replacement nomis id", example = "G4268VF")
  val newNomisId: String,

  @field:Schema(description = "The new booking id", example = "324")
  val newBookingId: Long,
)
