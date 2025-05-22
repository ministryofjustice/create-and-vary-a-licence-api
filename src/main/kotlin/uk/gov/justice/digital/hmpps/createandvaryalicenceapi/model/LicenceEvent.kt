package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
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
  val licenceId: Long? = null,

  @Schema(description = "The licence event type", example = "LicenceEventType.VARIATION_SUBMITTED")
  val eventType: LicenceEventType? = null,

  @Schema(
    description = "The username related to this event or SYSTEM if an automated event",
    example = "X63533",
  )
  val username: String? = null,

  @Schema(
    description = "The forename of the person related to this event, or SYSTEM if an automated event.",
    example = "Joe",
  )
  val forenames: String? = null,

  @Schema(
    description = "The surname of the person related to this event, or SYSTEM if an automated event.",
    example = "Bloggs",
  )
  val surname: String? = null,

  @Schema(description = "A reason or description related to the event", example = "Reason for variation")
  val eventDescription: String? = null,

  @Schema(description = "The date and time of the event", example = "12/01/2022 23:14:23")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val eventTime: LocalDateTime? = null,
)
