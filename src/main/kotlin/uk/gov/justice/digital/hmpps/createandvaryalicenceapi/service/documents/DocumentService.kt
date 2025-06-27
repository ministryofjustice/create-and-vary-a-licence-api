package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.documents

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DocumentService(
  private val apiClient: DocumentApiClient,
  @Value("\${hmpps.document.api.enabled:false}")
  private val documentServiceEnabled: Boolean = false,
) {

  fun uploadDocument(metadata: Map<String, String> = mapOf(), file: ByteArray): UUID? {
    if (!documentServiceEnabled) return null

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
