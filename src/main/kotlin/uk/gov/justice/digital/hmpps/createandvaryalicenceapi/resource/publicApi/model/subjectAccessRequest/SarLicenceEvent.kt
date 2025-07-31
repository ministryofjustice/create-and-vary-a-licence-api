package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes an event that was related to a licence")
data class SarLicenceEvent(
  @field:Schema(
    description = "The internal ID of the licence that this event relates to",
    example = "1234",
  )
  val licenceId: Long? = null,

  @field:Schema(description = "The licence event type", example = "Created")
  val eventType: SarLicenceEventType? = null,

  @field:Schema(
    description = "The username related to this event or SYSTEM if an automated event",
    example = "X63533",
  )
  val username: String? = null,

  @field:Schema(
    description = "The forename of the person related to this event, or SYSTEM if an automated event.",
    example = "Joe",
  )
  val forenames: String? = null,

  @field:Schema(
    description = "The surname of the person related to this event, or SYSTEM if an automated event.",
    example = "Bloggs",
  )
  val surname: String? = null,

  @field:Schema(description = "A reason or description related to the event", example = "Reason for variation")
  val eventDescription: String? = null,

  @field:Schema(description = "The date and time of the event", example = "12/01/2022 23:14:23")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val eventTime: LocalDateTime? = null,
)
