package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The list of licences, audit events and licence events")
data class Content(
  @field:Schema(description = "The list of licence events")
  val licences: List<SarLicence>,

  @field:Schema(description = "The list of audit events")
  val auditEvents: List<SarAuditEvent>,
)

@Schema(description = "The Sar Content holds the prisoner details")
data class SarContent(
  @field:Schema(description = "SAR content")
  val content: Content,

  @field:Schema(description = "The list of referenced attachments")
  val attachments: List<SarAttachmentDetail>,
)
