package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import java.util.*

@Service
class DocumentService(
  val documentApiClient: DocumentApiClient,
) {
  fun postFileToDocumentService(
    file: ByteArray?,
    fileType: MediaType,
    metadata: DocumentMetaData,
    documentType: String,
  ): String? {
    if (file == null || file.isEmpty()) return null

    val documentUuid = UUID.randomUUID().toString()
    documentApiClient.postDocument(documentUuid, file, fileType, metadata, documentType)
    return documentUuid
  }
}
