package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes when a decision was made to create a licence in NOMIS instead of CVL")
data class SarExternalRecord(

  @field:Schema(description = "The prison identifier for the person on this licence", example = "A9999AA")
  val prisonNumber: String,

  @field:Schema(description = "The reason for creating a licence in NOMIS", example = "A9999AA")
  var reason: String,

  @field:Schema(description = "The code that determines which prison made this decision", example = "MDI")
  var prisonCode: String,

  @field:Schema(description = "The date and time that this record was first created", example = "24/08/2022 09:30:33")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  val dateCreated: LocalDateTime,

  @field:Schema(description = "The date and time that this record was first created", example = "24/08/2022 09:30:33")
  @field:JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
  var dateLastUpdated: LocalDateTime,
)
