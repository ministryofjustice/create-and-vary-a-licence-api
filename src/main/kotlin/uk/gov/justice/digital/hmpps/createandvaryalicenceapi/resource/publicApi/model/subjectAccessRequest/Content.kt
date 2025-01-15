package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonView
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Views

@Schema(description = "The list of licences, audit events and licence events")
data class Content(
  @Schema(description = "The list of licence events")
  val licences: List<SarLicence>,

  @Schema(description = "The list of audit events")
  val auditEvents: List<SarAuditEvent>,

  @Schema(description = "The list of licence events")
  val licencesEvents: List<SarLicenceEvent>,
)

@Schema(description = "The Sar Content holds the prisoner details")
data class SarContent(
  @Schema(description = "SAR content")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val content: Content,
)
