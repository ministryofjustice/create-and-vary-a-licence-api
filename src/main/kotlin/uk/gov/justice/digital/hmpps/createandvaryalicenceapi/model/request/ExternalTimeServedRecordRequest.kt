package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object with information for NOMIS Time Served licence")
data class ExternalTimeServedRecordRequest(
  @field:Schema(description = "The reason for creating this licence", example = "Time served release")
  @field:NotBlank(message = "reason must not be blank")
  val reason: String,

  @field:Schema(description = "The prison code where the offender is located", example = "LEI")
  @field:NotBlank(message = "prisonCode must not be blank")
  val prisonCode: String,

)
