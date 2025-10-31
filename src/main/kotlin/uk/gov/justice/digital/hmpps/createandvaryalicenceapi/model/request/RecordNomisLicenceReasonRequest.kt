package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for creating a new NOMIS Time Served licence creation reason")
data class RecordNomisLicenceReasonRequest(
  @field:Schema(description = "The prison NOMIS identifier for this offender", example = "A1234AA")
  @field:NotBlank(message = "nomsId must not be blank")
  val nomsId: String,

  @field:Schema(description = "The booking ID associated with this offender", example = "123456")
  @field:NotNull(message = "bookingId must not be blank")
  val bookingId: Long,

  @field:Schema(description = "The reason for creating this licence", example = "Time served release")
  @field:NotBlank(message = "reason must not be blank")
  val reason: String,

  @field:Schema(description = "The prison code where the offender is located", example = "LEI")
  @field:NotBlank(message = "prisonCode must not be blank")
  val prisonCode: String,
)
