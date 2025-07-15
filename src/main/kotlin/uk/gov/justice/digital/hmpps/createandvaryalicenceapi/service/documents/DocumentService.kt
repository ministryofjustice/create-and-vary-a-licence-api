package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DocumentService(
  private val apiClient: DocumentApiClient,
) {
  fun uploadDocument(file: ByteArray, metadata: Map<String, String> = mapOf()): UUID {
    val documentUuid = UUID.randomUUID()

    apiClient.uploadDocument(
      documentUuid = documentUuid,
      documentType = DocumentType.EXCLUSION_ZONE_MAP,
      file = file,
      metadata = metadata,
    )

    return documentUuid
  }
}
