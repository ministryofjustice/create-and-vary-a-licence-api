package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.document

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.document.DocumentMetaData
import java.util.*

@Service
class DocumentService(
  val documentApiClient: DocumentApiClient,
) {
  fun postExclusionZoneMaps(
    file: ByteArray?,
    metadata: DocumentMetaData,
    documentType: String,
  ) {
    if (file == null || file.isEmpty()) return

    val documentUuid = UUID.randomUUID().toString()
    documentApiClient.postDocument(documentUuid, file.toString(Charsets.UTF_8), metadata, documentType)
  }
}
