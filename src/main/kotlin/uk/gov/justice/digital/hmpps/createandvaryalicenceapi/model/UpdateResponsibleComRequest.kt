package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

@Schema(description = "Request object for updating the COM responsible for an offender")
data class UpdateResponsibleComRequest(

  @Schema(description = "The unique identifier of the responsible COM, retrieved from Delius", example = "22003829")
  @field:NotNull
  val staffIdentifier: Long,

  @Schema(description = "The Delius username for the responsible COM", example = "jbloggs")
  @field:NotNull
  val staffUsername: String,

  @Schema(description = "The email address of the responsible COM", example = "jbloggs@probation.gov.uk")
  @field:NotNull
  val staffEmail: String,
)
