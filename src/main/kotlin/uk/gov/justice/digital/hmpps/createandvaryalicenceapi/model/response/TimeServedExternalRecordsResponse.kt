package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class TimeServedExternalRecordsResponse(
  @field:Schema(description = "The NOMIS identifier for the offender", example = "A1234AA")
  val nomsId: String,

  @field:Schema(description = "The booking ID associated with the offender", example = "123456")
  val bookingId: Long,

  @field:Schema(description = "The reason why the licence was created in NOMIS", example = "Time served release")
  val reason: String,

  @field:Schema(description = "The prison code where the offender is located", example = "LEI")
  val prisonCode: String,

  @field:Schema(description = "The date when this record was created", example = "2025-10-29T16:00:00Z")
  val dateCreated: LocalDateTime,

  @field:Schema(description = "The date when this record was last updated", example = "2025-10-29T16:00:00Z")
  val dateLastUpdated: LocalDateTime,
)
