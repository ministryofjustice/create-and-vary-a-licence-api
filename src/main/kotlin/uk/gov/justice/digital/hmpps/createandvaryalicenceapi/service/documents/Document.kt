package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import java.time.LocalDateTime
import java.util.UUID

data class Document(
  val documentUuid: UUID,
  val documentType: DocumentType,
  val documentFilename: String,
  val filename: String,
  val fileExtension: String,
  val fileSize: Long,
  val fileHash: String,
  val mimeType: String,
  val metadata: Map<String, String>,
  val createdTime: LocalDateTime,
  val createdByServiceName: String,
  val createdByUsername: String?,
)
