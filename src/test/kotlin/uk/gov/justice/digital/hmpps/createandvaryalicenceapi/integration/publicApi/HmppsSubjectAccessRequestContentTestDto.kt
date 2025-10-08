package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest.Content
import uk.gov.justice.hmpps.kotlin.sar.Attachment

data class HmppsSubjectAccessRequestContentTestDto(
  val content: Content,
  val attachments: List<Attachment>?,
)
