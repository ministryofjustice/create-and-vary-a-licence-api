package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonView
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.time.LocalDateTime

@Schema(description = "Describes an event that was related to a licence")
data class LicenceEvent(
  @Schema(description = "The internal ID of the licence event", example = "1234")
  val id: Long? = null,

  @Schema(
    description = "The internal ID of the licence that this event relates to",
    example = "1234",
  )
  @get:JsonView(Views.PublicSar::class)
  val licenceId: Long? = null,

  @Schema(description = "The licence event type", example = "LicenceEventType.VARIATION_SUBMITTED")
  @get:JsonView(Views.PublicSar::class)
  val eventType: LicenceEventType? = null,

  @Schema(
    description = "The username related to this event or SYSTEM if an automated event",
    example = "X63533",
  )
  @get:JsonView(Views.PublicSar::class)
  val username: String? = null,

  @Schema(
    description = "The forename of the person related to this event, or SYSTEM if an automated event.",
    example = "Robert Mortimer",
  )
  @get:JsonView(Views.PublicSar::class)
  val forenames: String? = null,

  @Schema(
    description = "The surname of the person related to this event, or SYSTEM if an automated event.",
    example = "Robert Mortimer",
  )
  @get:JsonView(Views.PublicSar::class)
  val surname: String? = null,

  @Schema(description = "A reason or description related to the event", example = "Reason for variation")
  @get:JsonView(Views.PublicSar::class)
  val eventDescription: String? = null,

  @Schema(description = "The date and time of the event", example = "12/01/2022 23:14:23")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  @get:JsonView(Views.PublicSar::class)
  val eventTime: LocalDateTime? = null,
)
