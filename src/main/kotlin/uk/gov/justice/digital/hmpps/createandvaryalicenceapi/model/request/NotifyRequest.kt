package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for creating a new licence")
data class NotifyRequest(
  @Schema(description = "The name of the person to contact", example = "Joe Bloggs")
  val name: String?,

  @Schema(description = "The email address to send the notification to", example = "joebloggs@probation.gov.uk")
  val email: String?,
)
