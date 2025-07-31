package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

enum class LicenceType {
  PRRD,
  CRD,
  HARD_STOP,
  HDC,
}

@Schema(description = "Request object for creating a new licence")
data class CreateLicenceRequest(
  @field:Schema(description = "The prison nomis identifier for this offender", example = "A1234AA")
  @param:NotNull
  val nomsId: String,

  @field:Schema(description = "The type of licence to create", example = "CRD")
  val type: LicenceType = LicenceType.CRD,
)
