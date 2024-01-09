package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for creating a new licence")
data class CreateLicenceRequest(
  @Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  @NotNull
  val nomsId: String,
)
