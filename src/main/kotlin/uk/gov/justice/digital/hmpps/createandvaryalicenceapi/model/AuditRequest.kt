package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes an audit event request")
data class AuditRequest(
  @field:Schema(description = "The internal ID of the licence to request audit events for", example = "1234")
  val licenceId: Long? = null,

  @field:Schema(description = "Username to request events for", example = "X63533")
  val username: String? = null,

  @field:Schema(
    description = "The start date and time to query for events (default is 1 month ago)",
    example = "13/11/2021 23:14:13",
  )
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val startTime: LocalDateTime? = LocalDateTime.now().minusMonths(1),

  @field:Schema(description = "The end time to query for events (default is now)", example = "12/01/2022 23:14:13")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val endTime: LocalDateTime? = LocalDateTime.now(),
)
