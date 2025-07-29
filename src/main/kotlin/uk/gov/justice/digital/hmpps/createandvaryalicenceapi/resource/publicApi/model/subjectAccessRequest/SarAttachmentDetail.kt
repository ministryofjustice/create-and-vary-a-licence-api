package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

data class SarAttachmentDetail(
  val attachmentNumber: Int,
  val name: String,
  val contentType: String,
  val url: String,
  val filename: String,
  val filesize: Int,
)

interface SarAttachmentSummary {
  val attachmentNumber: Int
}
