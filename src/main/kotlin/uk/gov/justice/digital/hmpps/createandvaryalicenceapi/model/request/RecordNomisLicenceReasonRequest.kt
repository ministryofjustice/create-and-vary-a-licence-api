package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for creating a new NOMIS Time Served licence creation reason")
data class RecordNomisLicenceReasonRequest(
  @field:Schema(description = "The prison NOMIS identifier for this offender", example = "A1234AA")
  @field:NotNull
  val nomsId: String,

  @field:Schema(description = "The booking ID associated with this offender", example = "123456")
  @field:NotNull
  val bookingId: Int,

  @field:Schema(description = "The reason for creating this licence", example = "Time served release")
  @field:NotNull
  val reason: String,

  @field:Schema(description = "The prison code where the offender is located", example = "LEI")
  val prisonCode: String,

  @field:Schema(description = "The ID of the case administrator who updated this licence", example = "42")
  @field:NotNull
  val updatedByCaId: Long,
)
