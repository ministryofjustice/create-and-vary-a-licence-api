package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DocumentService(
  private val apiClient: DocumentApiClient,
) {
  fun uploadDocument(file: ByteArray, metadata: Map<String, String> = mapOf()): UUID {
    val documentUuid = UUID.randomUUID()

    log.info("Uploading document: uuid={}", documentUuid)

    apiClient.uploadDocument(
      documentUuid = documentUuid,
      documentType = DocumentType.EXCLUSION_ZONE_MAP,
      file = file,
      metadata = metadata,
    )

    return documentUuid
  }

  fun downloadDocument(uuid: UUID): ByteArray {
    log.info("Downloading document: uuid={}", uuid)

    return apiClient.downloadDocumentFile(uuid)
  }

  fun deleteDocument(uuid: UUID) {
    log.info("Deleting document: uuid={}", uuid)

    apiClient.deleteDocument(uuid)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
