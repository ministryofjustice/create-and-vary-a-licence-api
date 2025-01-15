package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDateTime

@Schema(description = "Describes an audit event request")
data class SarAuditEvent(
  @Schema(
    description = "The internal ID of the licence that this event related to, or null if unrelated to a licence",
    example = "1234",
  )
  val licenceId: Long? = null,

  @Schema(description = "The date and time of the event", example = "12/01/2022 23:14:23")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val eventTime: LocalDateTime = LocalDateTime.now(),

  @Schema(
    description = "Username who initiated the event, if a user event, or SYSTEM if an automated event",
    example = "X63533",
  )
  val username: String? = "SYSTEM",

  @Schema(
    description = "The full name of the person who performed this auditable event, or SYSTEM if an automated event.",
    example = "Robert Mortimer",
  )
  val fullName: String? = "SYSTEM",

  @Schema(description = "The event type. One of SYSTEM_EVENT or USER_EVENT", example = "User event")
  val eventType: SarAuditEventType = SarAuditEventType.USER_EVENT,

  @Schema(description = "A summary of the action taken", example = "Updated a bespoke condition")
  @NotEmpty
  val summary: String? = null,

  @Schema(description = "A detailed description of the action taken", example = "Updated a bespoke condition")
  val detail: String? = null,
)
