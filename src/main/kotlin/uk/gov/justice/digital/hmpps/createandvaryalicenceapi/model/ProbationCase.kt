package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the probation case details held for an offender")
data class ProbationCase(
  @field:Schema(description = "The case reference number (CRN) for the person on this licence", example = "X12444")
  val crn: String,

  @field:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonNumber: String? = null,

  @field:Schema(description = "CRO Number", example = "29906/12J")
  val croNumber: String? = null,

  @field:Schema(description = "PNC Number", example = "12/394773H")
  val pncNumber: String? = null,
)
