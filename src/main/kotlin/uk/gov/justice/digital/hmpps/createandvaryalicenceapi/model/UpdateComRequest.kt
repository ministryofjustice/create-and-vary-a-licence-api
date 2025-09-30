package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for updating the COM responsible for an offender")
data class UpdateComRequest(

  @field:Schema(description = "The unique identifier of the COM, retrieved from Delius", example = "22003829")
  @field:NotNull
  val staffIdentifier: Long,

  @field:Schema(description = "The Delius staff code for the COM", example = "X012345")
  val staffCode: String? = null,

  @field:Schema(description = "The Delius username for the COM", example = "jbloggs")
  @field:NotNull
  val staffUsername: String,

  @field:Schema(description = "The email address of the COM", example = "jbloggs@probation.gov.uk")
  val staffEmail: String?,

  @field:Schema(description = "The first name of the COM", example = "Joseph")
  val firstName: String?,

  @field:Schema(description = "The last name of the COM", example = "Bloggs")
  val lastName: String?,
)
