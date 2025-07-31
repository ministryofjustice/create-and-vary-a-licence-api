package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Request object for updating a prison case administrator")
data class UpdatePrisonUserRequest(

  @field:Schema(description = "The NOMIS username of the case administrator", example = "jbloggs")
  @field:NotNull
  val staffUsername: String,

  @field:Schema(description = "The email address of the case administrator", example = "jbloggs@probation.gov.uk")
  val staffEmail: String?,

  @field:Schema(description = "The first name of the case administrator", example = "Joseph")
  val firstName: String?,

  @field:Schema(description = "The last name of the case administrator", example = "Bloggs")
  val lastName: String?,
)
