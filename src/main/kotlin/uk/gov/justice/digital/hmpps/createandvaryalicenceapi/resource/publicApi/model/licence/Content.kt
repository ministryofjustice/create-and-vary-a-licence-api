package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import com.fasterxml.jackson.annotation.JsonView
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Views
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.LicenceEvent

@Schema(description = "The list of licences, audit events and licence events")
data class Content(
  @Schema(description = "The list of licence events")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val licences: List<Licence>,

  @Schema(description = "The list of audit events")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val auditEvents: List<AuditEvent>,

  @Schema(description = "The list of licence events")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val licencesEvents: List<LicenceEvent>,
)

@Schema(description = "The Sar Content holds the prisoner details")
data class SarContent(
  @Schema(description = "SAR content")
  @get:JsonView(Views.SubjectAccessRequest::class)
  val content: Content,
)
