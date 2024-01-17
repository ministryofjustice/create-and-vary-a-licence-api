package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import java.util.*

@Service
class DocumentService(
  val documentApiClient: DocumentApiClient,
  @Value("\${document-service.enabled}") private val enabled: Boolean,
) {
  fun postFileToDocumentService(
    file: ByteArray?,
    fileType: MediaType,
    metadata: DocumentMetaData,
    documentType: String,
  ): String? {
    if (!enabled) {
      log.warn("Document Service is disabled: Didn't send document to document service.")
      return null
    }

    if (file == null || file.isEmpty()) return null

    val documentUuid = UUID.randomUUID().toString()
    documentApiClient.postDocument(documentUuid, file, fileType, metadata, documentType)
    return documentUuid
  }

  fun getDocument(documentUUID: String): ByteArray? {
    if (!enabled) {
      log.warn("Document Service is disabled.")
      return null
    }
    return documentApiClient.getDocument(documentUUID)
  }

  companion object {
    private val log = LoggerFactory.getLogger(DocumentService::class.java)
  }
}
