package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceEvent

@Schema(description = "The list of licences, audit events and licence events")
data class Content(
  @Schema(description = "The list of licences")
  val licences: List<Licence>,

  @Schema(description = "The list of audit events")
  val auditEvents: List<AuditEvent>,

  @Schema(description = "The list of licence events")
  val licencesEvents: List<LicenceEvent>,
)

@Schema(description = "The Sar Content holds the prisoner details")
data class SarContent(
  @Schema(description = "SAR content")
  val content: Content,
)
